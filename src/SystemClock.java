package ratelimiter;

public class SystemClock implements Clock {

    @Override
    public long now() {
        return System.nanoTime();
    }
}
