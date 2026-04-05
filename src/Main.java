package ratelimiter;

public class Main {

    public static void main(String[] args) throws InterruptedException {

        RateLimiterConfig config = new RateLimiterConfig(5, 1.0);
        RateLimiter limiter = new RateLimiter(config);

        System.out.println("=== Rate Limiter Demo ===");
        System.out.println("Config: " + config);
        System.out.println();

        System.out.println("Sending 8 rapid requests for user-1:");
        for (int i = 1; i <= 8; i++) {
            boolean allowed = limiter.allowRequest("user-1");
            System.out.println("  Request " + i + " : " + (allowed ? "ALLOWED" : "REJECTED"));
        }

        System.out.println();
        System.out.println("Waiting 3 seconds for token refill...");
        Thread.sleep(3000);

        System.out.println();
        System.out.println("Sending 4 more requests for user-1:");
        for (int i = 9; i <= 12; i++) {
            boolean allowed = limiter.allowRequest("user-1");
            System.out.println("  Request " + i + " : " + (allowed ? "ALLOWED" : "REJECTED"));
        }

        System.out.println();
        System.out.println("Sending 3 requests for user-2 (separate bucket):");
        for (int i = 1; i <= 3; i++) {
            boolean allowed = limiter.allowRequest("user-2");
            System.out.println("  Request " + i + " : " + (allowed ? "ALLOWED" : "REJECTED"));
        }

        System.out.println();
        System.out.println("=== Done ===");
    }
}
