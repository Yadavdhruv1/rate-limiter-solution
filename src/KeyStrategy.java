package ratelimiter;

public interface KeyStrategy {
    String resolveKey(Request request);
}
