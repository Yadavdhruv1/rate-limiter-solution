package ratelimiter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class FixedWindowCounter implements RateLimitPolicy {

    private final int maxRequests;
    private final long windowSizeMs;
    private final Clock clock;
    private final ConcurrentHashMap<String, WindowData> windows = new ConcurrentHashMap<>();

    public FixedWindowCounter(RateLimiterConfig config, Clock clock) {
        this.maxRequests = config.getMaxRequests();
        this.windowSizeMs = config.getWindowSizeMs();
        this.clock = clock;
    }

    @Override
    public boolean allowRequest(String key) {
        WindowData window = windows.computeIfAbsent(key, k -> new WindowData(clock.now()));
        synchronized (window) {
            long now = clock.now();
            long currentWindowStart = (now / windowSizeMs) * windowSizeMs;

            if (window.windowStart != currentWindowStart) {
                window.windowStart = currentWindowStart;
                window.count.set(0);
            }

            if (window.count.get() < maxRequests) {
                window.count.incrementAndGet();
                return true;
            }
            return false;
        }
    }

    private static class WindowData {
        long windowStart;
        AtomicInteger count = new AtomicInteger(0);

        WindowData(long windowStart) {
            this.windowStart = (windowStart / 1) * 1;
        }
    }
}
