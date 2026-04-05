package ratelimiter;

public class RateLimiterTest {

    static class FakeClock implements Clock {
        private long nanos = 0;

        @Override
        public long now() {
            return nanos;
        }

        void advanceSeconds(double seconds) {
            nanos += (long) (seconds * 1_000_000_000L);
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

    static void testAllowWithinCapacity() {
        System.out.println("Test: allow within capacity");
        FakeClock clock = new FakeClock();
        RateLimiterConfig cfg = new RateLimiterConfig(3, 1.0);
        RateLimiter rl = new RateLimiter(cfg, clock);

        check(true, rl.allowRequest("u1"), "req 1");
        check(true, rl.allowRequest("u1"), "req 2");
        check(true, rl.allowRequest("u1"), "req 3");
    }

    static void testRejectWhenEmpty() {
        System.out.println("Test: reject when empty");
        FakeClock clock = new FakeClock();
        RateLimiterConfig cfg = new RateLimiterConfig(2, 1.0);
        RateLimiter rl = new RateLimiter(cfg, clock);

        rl.allowRequest("u1");
        rl.allowRequest("u1");
        check(false, rl.allowRequest("u1"), "req 3 rejected");
        check(false, rl.allowRequest("u1"), "req 4 rejected");
    }

    static void testRefillAfterWait() {
        System.out.println("Test: refill after wait");
        FakeClock clock = new FakeClock();
        RateLimiterConfig cfg = new RateLimiterConfig(2, 1.0);
        RateLimiter rl = new RateLimiter(cfg, clock);

        rl.allowRequest("u1");
        rl.allowRequest("u1");
        check(false, rl.allowRequest("u1"), "empty");

        clock.advanceSeconds(2);

        check(true, rl.allowRequest("u1"), "after refill 1");
        check(true, rl.allowRequest("u1"), "after refill 2");
        check(false, rl.allowRequest("u1"), "empty again");
    }

    static void testTokensCappedAtCapacity() {
        System.out.println("Test: tokens capped at capacity");
        FakeClock clock = new FakeClock();
        RateLimiterConfig cfg = new RateLimiterConfig(3, 1.0);
        RateLimiter rl = new RateLimiter(cfg, clock);

        rl.allowRequest("u1");
        clock.advanceSeconds(100);

        check(true, rl.allowRequest("u1"), "req 1");
        check(true, rl.allowRequest("u1"), "req 2");
        check(true, rl.allowRequest("u1"), "req 3");
        check(false, rl.allowRequest("u1"), "req 4 rejected");
    }

    static void testPerUserIsolation() {
        System.out.println("Test: per user isolation");
        FakeClock clock = new FakeClock();
        RateLimiterConfig cfg = new RateLimiterConfig(1, 1.0);
        RateLimiter rl = new RateLimiter(cfg, clock);

        check(true, rl.allowRequest("alice"), "alice req 1");
        check(false, rl.allowRequest("alice"), "alice req 2 rejected");
        check(true, rl.allowRequest("bob"), "bob req 1 own bucket");
    }

    public static void main(String[] args) {
        System.out.println("=== RateLimiter Tests ===\n");

        testAllowWithinCapacity();
        testRejectWhenEmpty();
        testRefillAfterWait();
        testTokensCappedAtCapacity();
        testPerUserIsolation();

        System.out.println("\nPassed: " + passed);
        System.out.println("Failed: " + failed);

        if (failed > 0) System.exit(1);
    }
}
