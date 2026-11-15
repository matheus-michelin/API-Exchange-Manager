package com.manager.money.exchange.manager.security.token;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.manager.money.exchange.manager.User.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TokenServiceTest {

    private static final String SECRET = "test-secret-test-secret-test-secret";

    private TokenService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new TokenService();
        ReflectionTestUtils.setField(service, "secret", SECRET);
        user = mock(User.class);
        when(user.getLogin()).thenReturn("ana");
        when(user.getId()).thenReturn(7L);
    }

    @Test
    void rejectsMissingOrShortSecret() {
        var empty = new TokenService();
        assertThatThrownBy(empty::checkSecret).isInstanceOf(IllegalStateException.class);

        ReflectionTestUtils.setField(empty, "secret", "curto");
        assertThatThrownBy(empty::checkSecret).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void acceptsSecretWith32Characters() {
        service.checkSecret();
    }

    @Test
    void generatedTokenCarriesSubjectUidAndJti() {
        var jwt = service.verificar(service.gerarToken(user)).orElseThrow();

        assertThat(jwt.getSubject()).isEqualTo("ana");
        assertThat(jwt.getClaim("uid").asLong()).isEqualTo(7L);
        assertThat(jwt.getId()).isNotBlank();
        assertThat(jwt.getIssuer()).isEqualTo("Expense Manager");
    }

    @Test
    void eachTokenHasItsOwnJti() {
        var first = service.verificar(service.gerarToken(user)).orElseThrow().getId();
        var second = service.verificar(service.gerarToken(user)).orElseThrow().getId();

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void getSubjectReturnsTheLogin() {
        assertThat(service.getSubject(service.gerarToken(user))).contains("ana");
    }

    @Test
    void rejectsGarbage() {
        assertThat(service.verificar("abc")).isEmpty();
    }

    @Test
    void rejectsTokenSignedWithAnotherSecret() {
        var forged = JWT.create().withIssuer("Expense Manager").withSubject("ana")
                .withExpiresAt(Instant.now().plus(5, ChronoUnit.MINUTES))
                .sign(Algorithm.HMAC256("outro-segredo-outro-segredo-outro"));

        assertThat(service.verificar(forged)).isEmpty();
    }

    @Test
    void rejectsTokenFromAnotherIssuer() {
        var token = JWT.create().withIssuer("Outro").withSubject("ana")
                .withExpiresAt(Instant.now().plus(5, ChronoUnit.MINUTES))
                .sign(Algorithm.HMAC256(SECRET));

        assertThat(service.verificar(token)).isEmpty();
    }

    @Test
    void rejectsExpiredToken() {
        var token = JWT.create().withIssuer("Expense Manager").withSubject("ana")
                .withExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES))
                .sign(Algorithm.HMAC256(SECRET));

        assertThat(service.verificar(token)).isEmpty();
    }
}
