package ratelimiter;

public interface RateLimitPolicy {
    boolean allowRequest(String key);
}
