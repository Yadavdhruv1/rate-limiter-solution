package ratelimiter;

public class ExternalServiceProxy {

    private final RateLimiter rateLimiter;

    public ExternalServiceProxy(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    public String handleRequest(Request request, boolean needsExternalCall) {
        if (!needsExternalCall) {
            return "Handled internally, no external call needed";
        }

        if (!rateLimiter.allowRequest(request)) {
            return "REJECTED - rate limit exceeded";
        }

        return callExternalApi(request);
    }

    private String callExternalApi(Request request) {
        return "External API called successfully for customer: " + request.getCustomerId();
    }
}
