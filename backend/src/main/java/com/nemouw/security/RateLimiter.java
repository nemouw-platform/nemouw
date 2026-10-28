package com.nemouw.security;

import com.nemouw.common.config.AppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory DDoS shield: at most N requests per second per key (the client IP).
 * Counts live in one map per wall-clock second and the whole map is dropped when the second changes,
 * so memory cleans itself. One instance is shared by every security chain, so a client has a single budget.
 */
@Component
public class RateLimiter {
    private record Window(long second, ConcurrentHashMap<String, AtomicInteger> counts) {
    }

    private final int limitPerSecond;
    private final Clock clock;
    private volatile Window window;

    @Autowired
    RateLimiter(AppProperties props) {
        this(props.rateLimit().requestsPerSecond(), Clock.systemUTC());
    }

    RateLimiter(int limitPerSecond, Clock clock) {
        this.limitPerSecond = limitPerSecond;
        this.clock = clock;
        this.window = new Window(clock.instant().getEpochSecond(), new ConcurrentHashMap<>());
    }

    /** Counts this request and returns false once the key has used up its budget for the current second. */
    boolean tryAcquire(String key) {
        long second = clock.instant().getEpochSecond();
        Window current = window;
        if (current.second() != second) {
            synchronized (this) {
                current = window;
                if (current.second() != second) {
                    current = new Window(second, new ConcurrentHashMap<>());
                    window = current;
                }
            }
        }
        return current.counts().computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet() <= limitPerSecond;
    }
}