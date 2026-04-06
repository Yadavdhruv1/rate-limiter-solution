package ratelimiter;

public class CustomerKeyStrategy implements KeyStrategy {

    @Override
    public String resolveKey(Request request) {
        return "customer:" + request.getCustomerId();
    }
}
