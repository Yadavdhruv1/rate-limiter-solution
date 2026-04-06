package ratelimiter;

public class RateLimiter {

    private final RateLimitPolicy policy;
    private final KeyStrategy keyStrategy;

    public RateLimiter(RateLimitPolicy policy, KeyStrategy keyStrategy) {
        this.policy = policy;
        this.keyStrategy = keyStrategy;
    }

    public boolean allowRequest(Request request) {
        String key = keyStrategy.resolveKey(request);
        return policy.allowRequest(key);
    }
}
