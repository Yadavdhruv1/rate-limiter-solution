package ratelimiter;

public class Main {

    public static void main(String[] args) {

        RateLimiterConfig config = new RateLimiterConfig(5, 60000);
        Clock clock = new SystemClock();

        System.out.println("=== Pluggable Rate Limiter Demo ===");
        System.out.println("Config: " + config);
        System.out.println();

        // --- demo 1: fixed window with customer key ---
        System.out.println("--- Using Fixed Window Counter (by customer) ---");
        RateLimitPolicy fixedWindow = new FixedWindowCounter(config, clock);
        RateLimiter limiter1 = new RateLimiter(fixedWindow, new CustomerKeyStrategy());
        ExternalServiceProxy proxy1 = new ExternalServiceProxy(limiter1);

        Request req = new Request("C1", "T1", "key-abc", "stripe");

        for (int i = 1; i <= 7; i++) {
            String result = proxy1.handleRequest(req, true);
            System.out.println("  Request " + i + ": " + result);
        }

        System.out.println();
        System.out.println("  Internal request (no rate limit check): "
                + proxy1.handleRequest(req, false));

        // --- demo 2: sliding window with tenant key ---
        System.out.println();
        System.out.println("--- Using Sliding Window Counter (by tenant) ---");
        RateLimitPolicy slidingWindow = new SlidingWindowCounter(config, clock);
        RateLimiter limiter2 = new RateLimiter(slidingWindow, new TenantKeyStrategy());
        ExternalServiceProxy proxy2 = new ExternalServiceProxy(limiter2);

        for (int i = 1; i <= 7; i++) {
            String result = proxy2.handleRequest(req, true);
            System.out.println("  Request " + i + ": " + result);
        }

        // --- demo 3: token bucket with api key ---
        System.out.println();
        System.out.println("--- Using Token Bucket (by api key) ---");
        RateLimitPolicy tokenBucket = new TokenBucket(config, clock);
        RateLimiter limiter3 = new RateLimiter(tokenBucket, new ApiKeyStrategy());
        ExternalServiceProxy proxy3 = new ExternalServiceProxy(limiter3);

        for (int i = 1; i <= 7; i++) {
            String result = proxy3.handleRequest(req, true);
            System.out.println("  Request " + i + ": " + result);
        }

        // --- demo 4: same algorithm, different key strategies ---
        System.out.println();
        System.out.println("--- Different Key Strategies (Fixed Window, limit=2) ---");
        RateLimiterConfig tightConfig = new RateLimiterConfig(2, 60000);
        RateLimitPolicy policy = new FixedWindowCounter(tightConfig, clock);

        RateLimiter byCustomer = new RateLimiter(policy, new CustomerKeyStrategy());
        RateLimiter byProvider = new RateLimiter(policy, new ProviderKeyStrategy());

        Request r1 = new Request("C1", "T1", "k1", "stripe");
        Request r2 = new Request("C2", "T1", "k2", "stripe");

        System.out.println("  C1 req1 (by customer): " + byCustomer.allowRequest(r1));
        System.out.println("  C1 req2 (by customer): " + byCustomer.allowRequest(r1));
        System.out.println("  C1 req3 (by customer): " + byCustomer.allowRequest(r1));
        System.out.println("  C2 req1 (by customer): " + byCustomer.allowRequest(r2));

        System.out.println();
        System.out.println("  stripe req1 (by provider): " + byProvider.allowRequest(r1));
        System.out.println("  stripe req2 (by provider): " + byProvider.allowRequest(r2));
        System.out.println("  stripe req3 (by provider): " + byProvider.allowRequest(r1));

        System.out.println();
        System.out.println("=== Done ===");
    }
}
