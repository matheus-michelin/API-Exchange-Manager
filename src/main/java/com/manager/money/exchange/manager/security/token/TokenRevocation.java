package com.manager.money.exchange.manager.security.token;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class TokenRevocation {

    private final Cache<String, Boolean> revoked = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterWrite(25, TimeUnit.MINUTES)
            .build();

    public void revoke(String jti) {
        if (jti != null) revoked.put(jti, Boolean.TRUE);
    }

    public boolean isRevoked(String jti) {
        return jti != null && revoked.getIfPresent(jti) != null;
    }
}
