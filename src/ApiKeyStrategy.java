package ratelimiter;

public class ApiKeyStrategy implements KeyStrategy {

    @Override
    public String resolveKey(Request request) {
        return "apikey:" + request.getApiKey();
    }
}
