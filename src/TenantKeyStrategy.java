package ratelimiter;

public class TenantKeyStrategy implements KeyStrategy {

    @Override
    public String resolveKey(Request request) {
        return "tenant:" + request.getTenantId();
    }
}
