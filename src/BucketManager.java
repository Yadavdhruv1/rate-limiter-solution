package ratelimiter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BucketManager {

    private final Map<String, TokenBucket> buckets;
    private final RateLimiterConfig config;
    private final Clock clock;

    public BucketManager(RateLimiterConfig config, Clock clock) {
        this.buckets = new ConcurrentHashMap<>();
        this.config = config;
        this.clock = clock;
    }

    public TokenBucket getBucket(String userId) {
        return buckets.computeIfAbsent(userId, id ->
                new TokenBucket(config.getCapacity(), config.getRefillRate(), clock));
    }

    public int size() {
        return buckets.size();
    }
}
