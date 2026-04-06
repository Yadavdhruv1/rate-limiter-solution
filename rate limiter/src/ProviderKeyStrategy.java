package ratelimiter;

public class ProviderKeyStrategy implements KeyStrategy {

    @Override
    public String resolveKey(Request request) {
        return "provider:" + request.getProvider();
    }
}
