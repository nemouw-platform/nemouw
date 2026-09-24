package com.nemouw.http;

import spark.Spark;

import java.util.Set;

public class CORSFilter {

    // Exact-match production origins. Add staging/admin domains here.
    private static final Set<String> ALLOWED_ORIGINS = Set.of(
            "https://nemouw.com",
            "https://www.nemouw.com"
    );

    private static boolean isAllowed(String origin) {
        return origin != null && (ALLOWED_ORIGINS.contains(origin) || origin.startsWith("http://localhost:"));
    }

    public static void enableCORS() {
        Spark.before((request, response) -> {
            String origin = request.headers("Origin");

            if (isAllowed(origin)) {
                response.header("Access-Control-Allow-Origin", origin);
                response.header("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
                response.header("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With, Access-Control-Allow-Origin");
                response.header("Access-Control-Allow-Credentials", "true");
            }

            // Preflight is fully answered here, it never reaches a route
            if (request.requestMethod().equalsIgnoreCase("OPTIONS")) {
                response.status(200);
                Spark.halt(200);
            }
        });
    }
}