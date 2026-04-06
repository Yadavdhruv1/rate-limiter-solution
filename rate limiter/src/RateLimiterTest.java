package ratelimiter;

public class RateLimiterTest {

    static class FakeClock implements Clock {
        private long millis = 0;

        @Override
        public long now() {
            return millis;
        }

        void advanceMs(long ms) {
            millis += ms;
        }
    }

    private static int passed = 0;
    private static int failed = 0;

    private static void check(boolean expected, boolean actual, String label) {
        if (expected == actual) {
            System.out.println("  [PASS] " + label);
            passed++;
        } else {
            System.out.println("  [FAIL] " + label + " expected=" + expected + " got=" + actual);
            failed++;
        }
    }

    static void testFixedWindowAllowWithinLimit() {
        System.out.println("Test: FixedWindow - allow within limit");
        FakeClock clock = new FakeClock();
        RateLimiterConfig cfg = new RateLimiterConfig(3, 60000);
        FixedWindowCounter policy = new FixedWindowCounter(cfg, clock);

        check(true, policy.allowRequest("u1"), "req 1");
        check(true, policy.allowRequest("u1"), "req 2");
        check(true, policy.allowRequest("u1"), "req 3");
        check(false, policy.allowRequest("u1"), "req 4 rejected");
    }

    static void testFixedWindowResetsOnNewWindow() {
        System.out.println("Test: FixedWindow - resets on new window");
        FakeClock clock = new FakeClock();
        RateLimiterConfig cfg = new RateLimiterConfig(2, 60000);
        FixedWindowCounter policy = new FixedWindowCounter(cfg, clock);

        policy.allowRequest("u1");
        policy.allowRequest("u1");
        check(false, policy.allowRequest("u1"), "full");

        clock.advanceMs(60000);

        check(true, policy.allowRequest("u1"), "new window req 1");
        check(true, policy.allowRequest("u1"), "new window req 2");
        check(false, policy.allowRequest("u1"), "new window full");
    }

    static void testSlidingWindowAllowWithinLimit() {
        System.out.println("Test: SlidingWindow - allow within limit");
        FakeClock clock = new FakeClock();
        RateLimiterConfig cfg = new RateLimiterConfig(3, 60000);
        SlidingWindowCounter policy = new SlidingWindowCounter(cfg, clock);

        check(true, policy.allowRequest("u1"), "req 1");
        check(true, policy.allowRequest("u1"), "req 2");
        check(true, policy.allowRequest("u1"), "req 3");
        check(false, policy.allowRequest("u1"), "req 4 rejected");
    }

    static void testSlidingWindowWeightedCount() {
        System.out.println("Test: SlidingWindow - weighted count from previous window");
        FakeClock clock = new FakeClock();
        RateLimiterConfig cfg = new RateLimiterConfig(5, 60000);
        SlidingWindowCounter policy = new SlidingWindowCounter(cfg, clock);

        // send 4 reqs in first window (time 0)
        policy.allowRequest("u1");
        policy.allowRequest("u1");
        policy.allowRequest("u1");
        policy.allowRequest("u1");

        // move to halfway into second window
        clock.advanceMs(60000 + 30000);

        // weight from prev = 4 * 0.5 = 2, so effective = 2 + currentCount
        check(true, policy.allowRequest("u1"), "weighted allows (2 + 0 < 5)");
        check(true, policy.allowRequest("u1"), "weighted allows (2 + 1 < 5)");
        check(true, policy.allowRequest("u1"), "weighted allows (2 + 2 < 5)");
        check(false, policy.allowRequest("u1"), "weighted rejects (2 + 3 >= 5)");
    }

    static void testTokenBucketAllowAndReject() {
        System.out.println("Test: TokenBucket - allow and reject");
        FakeClock clock = new FakeClock();
        RateLimiterConfig cfg = new RateLimiterConfig(3, 60000);
        TokenBucket policy = new TokenBucket(cfg, clock);

        check(true, policy.allowRequest("u1"), "req 1");
        check(true, policy.allowRequest("u1"), "req 2");
        check(true, policy.allowRequest("u1"), "req 3");
        check(false, policy.allowRequest("u1"), "req 4 rejected");
    }

    static void testTokenBucketRefill() {
        System.out.println("Test: TokenBucket - refill after time");
        FakeClock clock = new FakeClock();
        RateLimiterConfig cfg = new RateLimiterConfig(2, 60000);
        TokenBucket policy = new TokenBucket(cfg, clock);

        policy.allowRequest("u1");
        policy.allowRequest("u1");
        check(false, policy.allowRequest("u1"), "empty");

        // 2 tokens per 60000ms = 1 token per 30000ms
        clock.advanceMs(30000);

        check(true, policy.allowRequest("u1"), "refilled after 30s");
    }

    static void testPerKeyIsolation() {
        System.out.println("Test: per key isolation");
        FakeClock clock = new FakeClock();
        RateLimiterConfig cfg = new RateLimiterConfig(1, 60000);
        FixedWindowCounter policy = new FixedWindowCounter(cfg, clock);

        check(true, policy.allowRequest("alice"), "alice allowed");
        check(false, policy.allowRequest("alice"), "alice rejected");
        check(true, policy.allowRequest("bob"), "bob has own bucket");
    }

    static void testKeyStrategy() {
        System.out.println("Test: key strategy pluggability");
        FakeClock clock = new FakeClock();
        RateLimiterConfig cfg = new RateLimiterConfig(1, 60000);
        FixedWindowCounter policy = new FixedWindowCounter(cfg, clock);

        RateLimiter byCustomer = new RateLimiter(policy, new CustomerKeyStrategy());
        Request r1 = new Request("C1", "T1", "k1", "stripe");
        Request r2 = new Request("C2", "T1", "k1", "stripe");

        check(true, byCustomer.allowRequest(r1), "C1 allowed");
        check(false, byCustomer.allowRequest(r1), "C1 rejected");
        check(true, byCustomer.allowRequest(r2), "C2 allowed (different key)");
    }

    static void testExternalCallOnlyRateLimited() {
        System.out.println("Test: only external calls are rate limited");
        FakeClock clock = new FakeClock();
        RateLimiterConfig cfg = new RateLimiterConfig(1, 60000);
        FixedWindowCounter policy = new FixedWindowCounter(cfg, clock);
        RateLimiter limiter = new RateLimiter(policy, new CustomerKeyStrategy());
        ExternalServiceProxy proxy = new ExternalServiceProxy(limiter);

        Request req = new Request("C1", "T1", "k1", "stripe");

        String r1 = proxy.handleRequest(req, false);
        check(true, r1.contains("internally"), "no external = no rate limit");

        String r2 = proxy.handleRequest(req, true);
        check(true, r2.contains("successfully"), "first external allowed");

        String r3 = proxy.handleRequest(req, true);
        check(true, r3.contains("REJECTED"), "second external rejected");

        String r4 = proxy.handleRequest(req, false);
        check(true, r4.contains("internally"), "non-external still works");
    }

    static void testAlgorithmSwap() {
        System.out.println("Test: swap algorithm without changing caller");
        FakeClock clock = new FakeClock();
        RateLimiterConfig cfg = new RateLimiterConfig(2, 60000);

        RateLimitPolicy fixed = new FixedWindowCounter(cfg, clock);
        RateLimitPolicy sliding = new SlidingWindowCounter(cfg, clock);
        RateLimitPolicy token = new TokenBucket(cfg, clock);

        KeyStrategy strategy = new CustomerKeyStrategy();
        Request req = new Request("C1", "T1", "k1", "stripe");

        RateLimiter limiter;

        limiter = new RateLimiter(fixed, strategy);
        limiter.allowRequest(req);
        limiter.allowRequest(req);
        check(false, limiter.allowRequest(req), "fixed window rejects");

        limiter = new RateLimiter(sliding, strategy);
        limiter.allowRequest(req);
        limiter.allowRequest(req);
        check(false, limiter.allowRequest(req), "sliding window rejects");

        limiter = new RateLimiter(token, strategy);
        limiter.allowRequest(req);
        limiter.allowRequest(req);
        check(false, limiter.allowRequest(req), "token bucket rejects");
    }

    public static void main(String[] args) {
        System.out.println("=== RateLimiter Tests ===\n");

        testFixedWindowAllowWithinLimit();
        testFixedWindowResetsOnNewWindow();
        testSlidingWindowAllowWithinLimit();
        testSlidingWindowWeightedCount();
        testTokenBucketAllowAndReject();
        testTokenBucketRefill();
        testPerKeyIsolation();
        testKeyStrategy();
        testExternalCallOnlyRateLimited();
        testAlgorithmSwap();

        System.out.println("\nPassed: " + passed);
        System.out.println("Failed: " + failed);

        if (failed > 0) System.exit(1);
    }
}
