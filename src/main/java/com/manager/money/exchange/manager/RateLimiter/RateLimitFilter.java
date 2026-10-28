package com.manager.money.exchange.manager.RateLimiter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
            "/geral/subtrair", new Rule("subtrair", 30, 60_000)
    );

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterAccess(10, TimeUnit.MINUTES) // maior que a maior janela (5 min)
            .build();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        return "OPTIONS".equalsIgnoreCase(req.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

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
                : "ip:" + req.getRemoteAddr() + ":" + rule.id();

        Bucket bucket = buckets.get(key, k -> new Bucket(rule.capacity(), rule.windowMs()));
        long wait = bucket.tryConsume();

        if (wait > 0) {
            res.setStatus(429);
            res.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(wait));
            res.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
            res.setCharacterEncoding("UTF-8");
            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
            res.getWriter().write("{\"campo\":null,\"mensagem\":\"Muitas requisições para este endpoint. Tente novamente em "
                    + wait + "s.\"}");
            return;
        }
        chain.doFilter(req, res);
    }

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
