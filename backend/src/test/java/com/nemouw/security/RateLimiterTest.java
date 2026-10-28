package com.nemouw.security;

import com.nemouw.support.Fixtures.MutableClock;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.nemouw.support.Fixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterTest {

    private static final String CLIENT_A = "203.0.113.7";
    private static final String CLIENT_B = "198.51.100.9";

    private final MutableClock clock = new MutableClock(NOW);

    private static boolean resultOf(Future<Boolean> future) {
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    @Nested
    class HappyPaths {

        @Test
        void allowsRequestsUpToTheLimitInOneSecond() {
            RateLimiter limiter = new RateLimiter(3, clock);

            List<Boolean> results = List.of(limiter.tryAcquire(CLIENT_A), limiter.tryAcquire(CLIENT_A),
                    limiter.tryAcquire(CLIENT_A));

            assertThat(results).containsOnly(true);
        }

        @Test
        void givesEachClientItsOwnBudget() {
            RateLimiter limiter = new RateLimiter(1, clock);
            limiter.tryAcquire(CLIENT_A);

            assertThat(limiter.tryAcquire(CLIENT_B)).isTrue();
        }

        @Test
        void restoresTheBudgetInTheNextSecond() {
            RateLimiter limiter = new RateLimiter(1, clock);
            limiter.tryAcquire(CLIENT_A);
            clock.advance(Duration.ofSeconds(1));

            assertThat(limiter.tryAcquire(CLIENT_A)).isTrue();
        }
    }

    @Nested
    class BoundaryValues {

        @Test
        void rejectsTheFirstRequestOverTheLimitAndKeepsRejecting() {
            RateLimiter limiter = new RateLimiter(2, clock);
            limiter.tryAcquire(CLIENT_A);
            limiter.tryAcquire(CLIENT_A);

            assertThat(List.of(limiter.tryAcquire(CLIENT_A), limiter.tryAcquire(CLIENT_A))).containsOnly(false);
        }

        @Test
        void allowsExactlyOneRequestWhenTheLimitIsOne() {
            RateLimiter limiter = new RateLimiter(1, clock);

            assertThat(List.of(limiter.tryAcquire(CLIENT_A), limiter.tryAcquire(CLIENT_A)))
                    .containsExactly(true, false);
        }

        @Test
        void keepsTheBudgetSpentUntilTheSecondActuallyChanges() {
            RateLimiter limiter = new RateLimiter(1, clock);
            limiter.tryAcquire(CLIENT_A);
            clock.advance(Duration.ofMillis(999));

            assertThat(limiter.tryAcquire(CLIENT_A)).isFalse();
        }

        @Test
        void rejectsEveryRequestWhenTheLimitIsZero() {
            RateLimiter limiter = new RateLimiter(0, clock);

            assertThat(limiter.tryAcquire(CLIENT_A)).isFalse();
        }
    }

    @Nested
    class Concurrency {

        @Test
        void letsExactlyTheLimitThroughWhen200RequestsArriveTogetherFromEightThreads() throws InterruptedException {
            RateLimiter limiter = new RateLimiter(50, clock);
            ExecutorService pool = Executors.newFixedThreadPool(8);
            List<Callable<Boolean>> requests = Collections.nCopies(200, () -> limiter.tryAcquire(CLIENT_A));

            try {
                long allowed = pool.invokeAll(requests).stream().filter(RateLimiterTest::resultOf).count();

                assertThat(allowed).isEqualTo(50);
            } finally {
                pool.shutdownNow();
            }
        }
    }
}
