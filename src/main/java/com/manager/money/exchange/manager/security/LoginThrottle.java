package com.manager.money.exchange.manager.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;


@Component
public class LoginThrottle {

    static final int MAX_FAILURES = 10;
    static final long WINDOW_MS = 15 * 60_000L;

    private static final class State {
        final long since = System.currentTimeMillis();
        int fails;
    }

    private final Cache<String, State> failures = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterWrite(WINDOW_MS, TimeUnit.MILLISECONDS)
            .build();

    /** Segundos até poder tentar de novo; 0 se o login não está bloqueado. */
    public long blockedSeconds(String login) {
        State s = failures.getIfPresent(login);
        if (s == null) return 0;
        synchronized (s) {
            if (s.fails < MAX_FAILURES) return 0;
            long left = WINDOW_MS - (System.currentTimeMillis() - s.since);
            return left > 0 ? (left + 999) / 1000 : 0;
        }
    }

    public void fail(String login) {
        State s = failures.get(login, k -> new State());
        synchronized (s) {
            s.fails++;
        }
    }

    public void reset(String login) {
        failures.invalidate(login);
    }
}
