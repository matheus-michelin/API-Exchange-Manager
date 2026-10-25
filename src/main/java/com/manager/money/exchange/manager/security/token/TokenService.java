package com.manager.money.exchange.manager.security.token;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.manager.money.exchange.manager.User.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class TokenService {

    @Value("${api.security.token.secret}")
    private String secret;

    public String gerarToken(User user) {
        return JWT.create()
                .withIssuer("Expense Manager")
                .withSubject(user.getLogin())
                .withExpiresAt(Instant.now().plus(20, ChronoUnit.MINUTES))
                .sign(Algorithm.HMAC256(secret));
    }

    public Optional<String> getSubject(String token) {
        try {
            return Optional.of(JWT.require(Algorithm.HMAC256(secret))
                    .withIssuer("Expense Manager")
                    .build()
                    .verify(token)
                    .getSubject());
        } catch (JWTVerificationException e) {
            return Optional.empty();
        }
    }
}
