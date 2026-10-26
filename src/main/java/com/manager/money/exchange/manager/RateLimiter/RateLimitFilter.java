package com.manager.money.exchange.manager.RateLimiter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int DEFAULT_API_PER_MIN = 60;
    private static final long DEFAULT_WINDOW_MS = 60_000;

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterAccess(10, TimeUnit.MINUTES) // Aumentado para respeitar janelas maiores (ex: 5 min)
            .build();

    private record RouteRule(String path, int capacity, long windowMs) {}

    private static final List<RouteRule> ROUTE_RULES = List.of(
            new RouteRule("/login/cadastro", 3, 300_000),
            new RouteRule("/login", 5, 300_000),
            new RouteRule("/geral/extrato", 10, 60_000),
            new RouteRule("/geral/adicionar", 30, 60_000),
            new RouteRule("/geral/subtrair", 30, 60_000)
    );

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String uri = req.getRequestURI();
        boolean login = uri.startsWith("/login");
        var auth = SecurityContextHolder.getContext().getAuthentication();
        boolean byUser = !login && auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);

        String clientIp = req.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = req.getRemoteAddr();
        } else {
            clientIp = clientIp.split(",")[0].trim();
        }

        RouteRule matchedRule = null;
        for (RouteRule rule : ROUTE_RULES) {
            if (uri.startsWith(rule.path())) {
                matchedRule = rule;
                break;
            }
        }

        int capacity = (matchedRule != null) ? matchedRule.capacity() : DEFAULT_API_PER_MIN;
        long windowMs = (matchedRule != null) ? matchedRule.windowMs() : DEFAULT_WINDOW_MS;

        String key = byUser ? "user:" + auth.getName() + ":" + uri
                : (login ? "login:" : "ip:") + clientIp + ":" + uri;


        Bucket bucket = buckets.get(key, k -> new Bucket(capacity, windowMs));
        long wait = bucket.tryConsume();

        if (wait > 0) {
            res.setStatus(429);
            res.setHeader("Retry-After", String.valueOf(wait));
            res.setCharacterEncoding("UTF-8");
            res.setContentType("application/json");
            res.getWriter().write("{\"error\": \"Muitas requisições para este endpoint. Tente novamente em " + wait + "s.\"}");
            return;
        }
        chain.doFilter(req, res);
    }
}