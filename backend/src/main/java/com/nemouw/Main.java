package com.nemouw;

import com.nemouw.http.ErrorHandling;

import static com.nemouw.middleware.CORSFilter.enableCORS;
import static com.nemouw.database.DatabaseInit.initializeDB;
import static com.nemouw.middleware.RateLimitFilter.runDdosShield;
import static spark.Spark.*;

public class Main {

    public static void main(String[] args) {
        // fail fast, before touching the DB or opening a port
        Config.validate();

        // Initialize the database first. If it crashes, the server never opens a dead port.
        initializeDB();

        // Open HTTP Server Ports and Configurations
        port(4567);

        ErrorHandling.install();

        enableCORS();
        runDdosShield();

        // Tells all bots to stay out of API subdomain entirely
        get("/robots.txt", (request, response) -> {
            response.type("text/plain");
            return "User-agent: *\nDisallow: /";
        });

        AppModule.registerRoutes();
    }
}