package com.nemouw;

import static com.nemouw.http.CORSFilter.enableCORS;
import static spark.Spark.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.nemouw.database.DatabaseInit.initializeDB;

public class Main {

    public static void main(String[] args) {
        // Initialize the database first. If it crashes, the server never opens a dead port.
        initializeDB();

        // Open HTTP Server Ports and Configurations
        port(4567);

        enableCORS();
        runDdosShield();

        // Simple Endpoint to verify Spark Java server is alive
        get("/api/health", (req, res) -> {
            res.type("application/json");
            return "{\"status\":\"healthy\",\"server\":\"connected\",\"database\":\"active\"}";
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

        // Background thread to reset the rate limits every minute
        Thread cleanerThread = createCleanerThread(accessPerMinute);
        cleanerThread.start();
    }

    private static Thread createCleanerThread(Map<String, Integer> accessPerMinute) {
        Thread cleanerThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(60000);
                    accessPerMinute.clear();
                } catch (InterruptedException e) {
                    // Re-interrupt the thread and exit loop cleanly instead of burning CPU 100%
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        cleanerThread.setDaemon(true); // Allows JVM to shut down cleanly if needed
        return cleanerThread;
    }
}
