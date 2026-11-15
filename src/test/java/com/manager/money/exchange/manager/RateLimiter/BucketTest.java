package com.manager.money.exchange.manager.RateLimiter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BucketTest {

    @Test
    void allowsUpToTheCapacityThenBlocks() {
        var bucket = new Bucket(3, 60_000);

        assertThat(bucket.tryConsume()).isZero();
        assertThat(bucket.tryConsume()).isZero();
        assertThat(bucket.tryConsume()).isZero();
        assertThat(bucket.tryConsume()).isPositive();
    }

    @Test
    void waitIsAtMostOneRefillInterval() {
        var bucket = new Bucket(2, 60_000); // 1 token a cada 30 s
        bucket.tryConsume();
        bucket.tryConsume();

        assertThat(bucket.tryConsume()).isBetween(1L, 30L);
    }

    @Test
    void bucketsAreIndependent() {
        var first = new Bucket(1, 60_000);
        var second = new Bucket(1, 60_000);
        first.tryConsume();

        assertThat(first.tryConsume()).isPositive();
        assertThat(second.tryConsume()).isZero();
    }
}
