package com.manager.money.exchange.manager.RateLimiter;

public final class Bucket {
    private final int capacity;
    private final long windowNanos;
    private final double perNano;
    private double tokens;
    private long last = System.nanoTime();

    public Bucket(int capacity, long windowMs) {
        this.capacity = capacity;
        this.windowNanos = windowMs * 1_000_000L;
        this.perNano = (double) capacity / windowNanos;
        this.tokens = capacity;
    }

    public synchronized long tryConsume() {
        long now = System.nanoTime();
        long elapsed = now - last;
        last = now;

        tokens = Math.min(capacity, tokens + elapsed * perNano);

        if (tokens >= 1.0) {
            tokens -= 1.0;
            return 0;
        }

        double missingTokens = 1.0 - tokens;
        double nanosToWait = missingTokens / perNano;
        return (long) Math.ceil(nanosToWait / 1_000_000_000.0);
    }
}
