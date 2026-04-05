package ratelimiter;

import java.util.concurrent.locks.ReentrantLock;

public class TokenBucket implements RateLimitPolicy {

    private final int capacity;
    private double tokens;
    private final double refillRate;
    private long lastRefillTime;
    private final ReentrantLock lock;
    private final Clock clock;

    public TokenBucket(int capacity, double refillRate, Clock clock) {
        this.capacity = capacity;
        this.tokens = capacity;
        this.refillRate = refillRate;
        this.clock = clock;
        this.lastRefillTime = clock.now();
        this.lock = new ReentrantLock();
    }

    @Override
    public boolean allowRequest() {
        lock.lock();
        try {
            refill();
            if (tokens >= 1) {
                tokens -= 1;
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    private void refill() {
        long now = clock.now();
        double elapsed = (now - lastRefillTime) / 1_000_000_000.0;
        if (elapsed > 0) {
            tokens = Math.min(capacity, tokens + elapsed * refillRate);
            lastRefillTime = now;
        }
    }

    public double getTokens() {
        return tokens;
    }

    public int getCapacity() {
        return capacity;
    }
}
