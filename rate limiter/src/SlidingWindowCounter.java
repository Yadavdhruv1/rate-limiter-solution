package ratelimiter;

import java.util.concurrent.ConcurrentHashMap;

public class SlidingWindowCounter implements RateLimitPolicy {

    private final int maxRequests;
    private final long windowSizeMs;
    private final Clock clock;
    private final ConcurrentHashMap<String, SlidingWindowData> windows = new ConcurrentHashMap<>();

    public SlidingWindowCounter(RateLimiterConfig config, Clock clock) {
        this.maxRequests = config.getMaxRequests();
        this.windowSizeMs = config.getWindowSizeMs();
        this.clock = clock;
    }

    @Override
    public boolean allowRequest(String key) {
        SlidingWindowData data = windows.computeIfAbsent(key, k -> new SlidingWindowData());
        synchronized (data) {
            long now = clock.now();
            long currentWindowStart = (now / windowSizeMs) * windowSizeMs;
            long elapsed = now - currentWindowStart;

            if (data.currentWindowStart != currentWindowStart) {
                if (currentWindowStart - data.currentWindowStart == windowSizeMs) {
                    data.previousCount = data.currentCount;
                } else {
                    data.previousCount = 0;
                }
                data.currentCount = 0;
                data.currentWindowStart = currentWindowStart;
            }

            double weight = data.previousCount * ((windowSizeMs - elapsed) / (double) windowSizeMs)
                            + data.currentCount;

            if (weight < maxRequests) {
                data.currentCount++;
                return true;
            }
            return false;
        }
    }

    private static class SlidingWindowData {
        long currentWindowStart = 0;
        int currentCount = 0;
        int previousCount = 0;
    }
}
