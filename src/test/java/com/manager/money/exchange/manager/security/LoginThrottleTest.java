package com.manager.money.exchange.manager.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginThrottleTest {

    private final LoginThrottle throttle = new LoginThrottle();

    private void fail(String login, int times) {
        for (int i = 0; i < times; i++) throttle.fail(login);
    }

    @Test
    void unknownLoginIsNotBlocked() {
        assertThat(throttle.blockedSeconds("ana")).isZero();
    }

    @Test
    void notBlockedBelowTheLimit() {
        fail("ana", LoginThrottle.MAX_FAILURES - 1);

        assertThat(throttle.blockedSeconds("ana")).isZero();
    }

    @Test
    void blockedAtTheLimit() {
        fail("ana", LoginThrottle.MAX_FAILURES);

        assertThat(throttle.blockedSeconds("ana")).isBetween(1L, LoginThrottle.WINDOW_MS / 1000);
    }

    @Test
    void successfulLoginResetsTheCount() {
        fail("ana", LoginThrottle.MAX_FAILURES);

        throttle.reset("ana");

        assertThat(throttle.blockedSeconds("ana")).isZero();
    }

    @Test
    void loginsAreIndependent() {
        fail("ana", LoginThrottle.MAX_FAILURES);

        assertThat(throttle.blockedSeconds("bia")).isZero();
    }
}
