package com.manager.money.exchange.manager.security.token;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenRevocationTest {

    private final TokenRevocation revocation = new TokenRevocation();

    @Test
    void revokedJtiIsReported() {
        revocation.revoke("jti-1");

        assertThat(revocation.isRevoked("jti-1")).isTrue();
    }

    @Test
    void otherJtiIsNotAffected() {
        revocation.revoke("jti-1");

        assertThat(revocation.isRevoked("jti-2")).isFalse();
    }

    @Test
    void nullJtiIsIgnored() {
        revocation.revoke(null);

        assertThat(revocation.isRevoked(null)).isFalse();
    }
}
