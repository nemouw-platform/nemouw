package com.nemouw.middleware;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.nemouw.Config;
import com.nemouw.error.Errors;
import com.nemouw.http.ClientIp;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static spark.Spark.before;

public class RateLimitFilter {
    /**
     * In-memory DDoS Rate Limiter
     */
    public static void runDdosShield() {
        // Enforce the rate limit strictly per-second to prevent bursts
        int requestLimitPerSecond = Config.getDdosShieldRequestLimitPerSecond();
        int retryWindowSeconds = 1;

        // Self-cleaning memory shield. IPs are completely erased from RAM exactly 1 second after they stop attacking.
        Cache<String, AtomicInteger> accessPerSecond = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.SECONDS)
                .build();

        before("/*", (request, response) -> {
            if (request.requestMethod().equalsIgnoreCase("OPTIONS")) {
                return;
            }

            String ip = ClientIp.resolve(request);

            AtomicInteger counter = accessPerSecond.get(ip, key -> new AtomicInteger(0));
            int currentAccessCount = counter.incrementAndGet();
            if (currentAccessCount > requestLimitPerSecond) {
                throw Errors.tooManyRequests(retryWindowSeconds);
            }
        });
    }
}