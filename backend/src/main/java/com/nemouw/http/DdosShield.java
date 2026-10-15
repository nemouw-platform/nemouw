package com.nemouw.http;

import com.nemouw.Config;

import java.util.Map;
import java.util.concurrent.*;

import static spark.Spark.before;
import static spark.Spark.halt;

public final class DdosShield {
    private DdosShield() {}

    public static void install() {
        // use whichever getter name your Config has
        int requestLimitPerMinute = Config.getDdosShieldRequestLimitPerSecond() * 60;
        Map<String, Integer> accessPerMinute = new ConcurrentHashMap<>();

        before("/*", (request, response) -> {
            String ip = ClientIp.resolve(request);
            if (ip == null) ip = request.ip();
            int current = accessPerMinute.merge(ip, 1, Integer::sum);
            if (current > requestLimitPerMinute) halt(429, "Too many requests");
        });

        ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ddos-shield-cleaner");
            t.setDaemon(true);
            return t;
        });
        cleaner.scheduleAtFixedRate(accessPerMinute::clear, 1, 1, TimeUnit.MINUTES);
    }
}