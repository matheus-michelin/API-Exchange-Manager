package com.manager.money.exchange.manager.security.token;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.manager.money.exchange.manager.User.User;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class TokenService {

    @Value("${api.security.token.secret}")
    private String secret;

    /** Segredo vazio ou curto deixaria qualquer pessoa forjar tokens: a API não sobe assim. */
    @PostConstruct
    void checkSecret() {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET precisa ter pelo menos 32 caracteres");
        }
    }

    public String gerarToken(User user) {
        return JWT.create()
                .withIssuer("Expense Manager")
                .withSubject(user.getLogin())
                .withClaim("uid", user.getId()) // amarra o token ao id: login reaproveitado após excluir a conta não herda tokens
                .withJWTId(UUID.randomUUID().toString()) // permite cancelar o token no "Sair"
                .withExpiresAt(Instant.now().plus(20, ChronoUnit.MINUTES))
                .sign(Algorithm.HMAC256(secret));
    }

    public Optional<String> getSubject(String token) {
        return verificar(token).map(DecodedJWT::getSubject);
    }


    public Optional<DecodedJWT> verificar(String token) {
        try {
            return Optional.of(JWT.require(Algorithm.HMAC256(secret))
                    .withIssuer("Expense Manager")
                    .build()
                    .verify(token));
        } catch (JWTVerificationException e) {
            return Optional.empty();
        }
    }
}
