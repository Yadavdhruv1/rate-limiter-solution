package ratelimiter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class TokenBucket implements RateLimitPolicy {

    private final int capacity;
    private final double refillRatePerMs;
    private final Clock clock;
    private final ConcurrentHashMap<String, BucketData> buckets = new ConcurrentHashMap<>();

    public TokenBucket(RateLimiterConfig config, Clock clock) {
        this.capacity = config.getMaxRequests();
        this.refillRatePerMs = config.getMaxRequests() / (double) config.getWindowSizeMs();
        this.clock = clock;
    }

    @Override
    public boolean allowRequest(String key) {
        BucketData bucket = buckets.computeIfAbsent(key, k -> new BucketData(capacity, clock.now()));
        bucket.lock.lock();
        try {
            long now = clock.now();
            long elapsed = now - bucket.lastRefillTime;
            if (elapsed > 0) {
                bucket.tokens = Math.min(capacity, bucket.tokens + elapsed * refillRatePerMs);
                bucket.lastRefillTime = now;
            }

            if (bucket.tokens >= 1) {
                bucket.tokens -= 1;
                return true;
            }
            return false;
        } finally {
            bucket.lock.unlock();
        }
    }

    private static class BucketData {
        double tokens;
        long lastRefillTime;
        ReentrantLock lock = new ReentrantLock();

        BucketData(int capacity, long now) {
            this.tokens = capacity;
            this.lastRefillTime = now;
        }
    }
}
