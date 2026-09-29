package com.nemouw;

import static com.nemouw.http.CORSFilter.enableCORS;
import static spark.Spark.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.nemouw.database.DatabaseInit.initializeDB;

public class Main {

    public static void main(String[] args) {
        // Initialize the database first. If it crashes, the server never opens a dead port.
        initializeDB();

        // Open HTTP Server Ports and Configurations
        port(4567);

        enableCORS();
        runDdosShield();

        // Simple Endpoint to verify JDBI works
        get("/api/debug/roles", (req, res) -> {
            res.type("application/json");
            java.util.List<String> codes = com.nemouw.database.DatabaseConnection.jdbi().withHandle(handle ->
                    handle.createQuery("SELECT code FROM user_roles ORDER BY code")
                            .mapTo(String.class)
                            .list());
            return "[\"" + String.join("\",\"", codes) + "\"]";
        });
    }


    /**
     * In-memory DDoS Rate Limiter
     */
    private static void runDdosShield() {
        int requestLimitPerMinute = Config.getDdosShieldRequestLimitPerSecond() * 60;
        Map<String /*IP*/, Integer> accessPerMinute = new ConcurrentHashMap<>();

        before("/*", (request, response) -> {
            String ip = request.ip();

            // Atomic merge prevents race conditions across Spark threads
            int currentAccessCount = accessPerMinute.merge(ip, 1, Integer::sum);

            if (currentAccessCount > requestLimitPerMinute) {
                halt(429, "Too many requests");
            }
        });

        // Reset the rate limits every minute on a single daemon thread
        ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ddos-shield-cleaner");
            t.setDaemon(true); // Allows JVM to shut down cleanly if needed
            return t;
        });
        cleaner.scheduleAtFixedRate(accessPerMinute::clear, 1, 1, TimeUnit.MINUTES);
    }
}