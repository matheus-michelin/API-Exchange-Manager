package com.manager.money.exchange.manager.RateLimiter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * Rate limit por rota.
 *
 * IP do cliente atrás de proxy (Tailscale, Vercel, nginx...):
 *  - Se a conexão NÃO vem de um proxy listado em app.rate-limit.trusted-proxies, os cabeçalhos
 *    de IP são ignorados (qualquer um pode forjá-los) e vale getRemoteAddr().
 *  - Se vem de um proxy confiável, o IP do cliente é lido de app.rate-limit.client-ip-header.
 *    No X-Forwarded-For a lista é percorrida da DIREITA para a esquerda e vale a primeira entrada
 *    que não seja proxy confiável: a esquerda é escrita pelo cliente, a direita pelo seu proxy.
 *  - Valor ausente ou malformado: volta ao getRemoteAddr() e registra um aviso no log.
 *
 * Propriedades (opcionais):
 *   app.rate-limit.trusted-proxies=127.0.0.1,::1
 *   app.rate-limit.client-ip-header=X-Forwarded-For      (ou Tailscale-Ingress-Src, valor "ip:porta")
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private record Rule(String id, int capacity, long windowMs) {}

    private static final Rule LOGIN = new Rule("login", 5, 300_000);
    private static final Rule CADASTRO = new Rule("cadastro", 3, 300_000);

    private static final Rule DEFAULT_RULE = new Rule("default", 60, 60_000);

    private static final Map<String, Rule> RULES = Map.of(
            "/login", LOGIN,
            "/login/cadastro", CADASTRO,
            "/geral", new Rule("geral", 60, 60_000),
            "/geral/extrato", new Rule("extrato", 10, 60_000),
            "/geral/adicionar", new Rule("adicionar", 30, 60_000),
            "/geral/subtrair", new Rule("subtrair", 30, 60_000),
            "/conta/excluir", new Rule("excluir", 3, 300_000) // por usuário; barra tentar a senha em massa
    );

    // Nenhuma rota recebe mais que alguns campos de texto: 8 KB sobram e protegem a memória.
    private static final long MAX_BODY_BYTES = 8 * 1024;
    private static final int MAX_HEADER_LENGTH = 512;
    private static final Pattern IPV4 =
            Pattern.compile("^((25[0-5]|2[0-4]\\d|1?\\d?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1?\\d?\\d)$");
    private static final Pattern IPV6_CHARS = Pattern.compile("^[0-9a-f:.]+$");

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterAccess(10, TimeUnit.MINUTES) // maior que a maior janela (5 min)
            .build();

    private final Set<String> trustedProxies;
    private final String clientIpHeader;
    private final AtomicLong lastWarnMs = new AtomicLong();

    public RateLimitFilter(
            @Value("${app.rate-limit.trusted-proxies:127.0.0.1,::1}") List<String> trustedProxies,
            @Value("${app.rate-limit.client-ip-header:X-Forwarded-For}") String clientIpHeader) {
        Set<String> set = new HashSet<>();
        for (String p : trustedProxies) {
            if (p != null && !p.isBlank()) set.add(normalizeAddr(p));
        }
        this.trustedProxies = Set.copyOf(set);
        this.clientIpHeader = clientIpHeader.trim();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        return "OPTIONS".equalsIgnoreCase(req.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        long declared = req.getContentLengthLong();
        if (declared > MAX_BODY_BYTES) {
            res.setStatus(413);
            res.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
            res.setCharacterEncoding("UTF-8");
            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
            res.getWriter().write("{\"campo\":null,\"mensagem\":\"Corpo da requisição grande demais\"}");
            return;
        }
        // Sem Content-Length (chunked) o tamanho não é conhecido: limita enquanto o corpo é lido.
        if (declared < 0 && !"GET".equalsIgnoreCase(req.getMethod())) {
            req = new LimitedRequest(req);
        }

        String path = normalize(req);
        Rule rule = RULES.getOrDefault(path, DEFAULT_RULE);

        boolean loginRoute = rule == LOGIN || rule == CADASTRO;
        var auth = SecurityContextHolder.getContext().getAuthentication();
        boolean byUser = !loginRoute
                && auth != null
                && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken);

        // Login/cadastro: sempre por IP (o usuário ainda não existe/não está autenticado).
        String key = byUser
                ? "user:" + auth.getName() + ":" + rule.id()
                : "ip:" + bucketIp(clientIp(req)) + ":" + rule.id();

        Bucket bucket = buckets.get(key, k -> new Bucket(rule.capacity(), rule.windowMs()));
        long wait = bucket.tryConsume();

        if (wait > 0) {
            res.setStatus(429);
            res.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(wait));
            res.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
            res.setCharacterEncoding("UTF-8");
            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
            // "wait" é um long calculado pelo servidor: seguro de concatenar.
            res.getWriter().write("{\"campo\":null,\"mensagem\":\"Muitas requisições para este endpoint. Tente novamente em "
                    + wait + "s.\"}");
            return;
        }
        chain.doFilter(req, res);
    }

    /** IP do cliente conforme a regra descrita no topo da classe. */
    String clientIp(HttpServletRequest req) {
        String remote = normalizeAddr(req.getRemoteAddr());
        if (!trustedProxies.contains(remote)) {
            return remote;
        }
        String raw = req.getHeader(clientIpHeader);
        String ip = (raw == null || raw.isBlank() || raw.length() > MAX_HEADER_LENGTH) ? null : pickClientIp(raw);
        if (ip == null) {
            warnMissingHeader();
            return remote;
        }
        return ip;
    }

    private String pickClientIp(String headerValue) {
        String[] parts = headerValue.split(",");
        for (int i = parts.length - 1; i >= 0; i--) {
            String candidate = stripPort(parts[i].trim());
            if (!isValidIp(candidate)) {
                return null; // valor estranho: não confiar em nada
            }
            candidate = normalizeAddr(candidate);
            if (!trustedProxies.contains(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    /** IPv6: um cliente costuma controlar um /64 inteiro; a chave usa só os 64 bits de rede. */
    static String bucketIp(String ip) {
        if (ip.indexOf(':') < 0) return ip;
        try {
            byte[] b = InetAddress.getByName(ip).getAddress(); // literal IPv6: sem consulta de DNS
            if (b.length != 16) return ip;
            return String.format("%02x%02x:%02x%02x:%02x%02x:%02x%02x::/64",
                    b[0], b[1], b[2], b[3], b[4], b[5], b[6], b[7]);
        } catch (UnknownHostException e) {
            return ip;
        }
    }

    /** Corta o corpo que passar de MAX_BODY_BYTES (só getInputStream: as rotas leem JSON por ele). */
    private static final class LimitedRequest extends HttpServletRequestWrapper {
        private ServletInputStream limited;

        LimitedRequest(HttpServletRequest r) {
            super(r);
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (limited == null) {
                ServletInputStream in = super.getInputStream();
                limited = new ServletInputStream() {
                    private long total;

                    private void count(long n) throws IOException {
                        if (n > 0 && (total += n) > MAX_BODY_BYTES) throw new IOException("Corpo grande demais");
                    }

                    @Override public int read() throws IOException {
                        int b = in.read();
                        if (b >= 0) count(1);
                        return b;
                    }

                    @Override public int read(byte[] b, int off, int len) throws IOException {
                        int n = in.read(b, off, len);
                        count(n);
                        return n;
                    }

                    @Override public boolean isFinished() { return in.isFinished(); }
                    @Override public boolean isReady() { return in.isReady(); }
                    @Override public void setReadListener(ReadListener l) { in.setReadListener(l); }
                };
            }
            return limited;
        }
    }

    private void warnMissingHeader() {
        long now = System.currentTimeMillis();
        long last = lastWarnMs.get();
        if (now - last > 60_000 && lastWarnMs.compareAndSet(last, now)) {
            logger.warn("Requisição de proxy confiável sem cabeçalho de IP válido ('" + clientIpHeader
                    + "'): todos os clientes estão dividindo o mesmo limite. Verifique o proxy ou "
                    + "app.rate-limit.client-ip-header.");
        }
    }

    private static String stripPort(String s) {
        if (s.startsWith("[")) {
            int end = s.indexOf(']');
            return end > 0 ? s.substring(1, end) : s;
        }
        int first = s.indexOf(':');
        if (first >= 0 && first == s.lastIndexOf(':')) {
            return s.substring(0, first); // ipv4:porta
        }
        return s;
    }

    private static boolean isValidIp(String s) {
        if (s.isEmpty() || s.length() > 45) return false;
        if (s.indexOf(':') < 0) return IPV4.matcher(s).matches();
        long colons = s.chars().filter(c -> c == ':').count();
        return colons >= 2 && colons <= 8 && IPV6_CHARS.matcher(s.toLowerCase(Locale.ROOT)).matches();
    }

    private static String normalizeAddr(String a) {
        if (a == null) return "unknown";
        String s = a.trim().toLowerCase(Locale.ROOT);
        if (s.equals("0:0:0:0:0:0:0:1")) return "::1";
        if (s.startsWith("::ffff:") && s.indexOf('.') > 0) return s.substring(7);
        return s;
    }

    /** Caminho sem context-path e sem barra final, para casar exatamente com as regras. */
    private static String normalize(HttpServletRequest req) {
        String uri = req.getRequestURI();
        String ctx = req.getContextPath();
        if (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx)) {
            uri = uri.substring(ctx.length());
        }
        int len = uri.length();
        while (len > 1 && uri.charAt(len - 1) == '/') {
            len--;
        }
        return uri.substring(0, len);
    }
}
