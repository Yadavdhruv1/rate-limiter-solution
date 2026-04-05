package ratelimiter;

public class RateLimiter {

    private final BucketManager bucketManager;
    private final RateLimiterConfig config;

    public RateLimiter(RateLimiterConfig config) {
        this(config, new SystemClock());
    }

    public RateLimiter(RateLimiterConfig config, Clock clock) {
        this.config = config;
        this.bucketManager = new BucketManager(config, clock);
    }

    public boolean allowRequest(String userId) {
        TokenBucket bucket = bucketManager.getBucket(userId);
        return bucket.allowRequest();
    }

    public RateLimiterConfig getConfig() {
        return config;
    }
}
