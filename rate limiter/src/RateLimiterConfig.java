package ratelimiter;

public class RateLimiterConfig {

    private final int maxRequests;
    private final long windowSizeMs;

    public RateLimiterConfig(int maxRequests, long windowSizeMs) {
        this.maxRequests = maxRequests;
        this.windowSizeMs = windowSizeMs;
    }

    public int getMaxRequests() {
        return maxRequests;
    }

    public long getWindowSizeMs() {
        return windowSizeMs;
    }

    @Override
    public String toString() {
        return "RateLimiterConfig{maxRequests=" + maxRequests + ", windowSizeMs=" + windowSizeMs + "}";
    }
}
