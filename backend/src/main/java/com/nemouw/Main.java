package com.nemouw;

import com.nemouw.database.DatabaseConnection;
import com.nemouw.service.DefaultEmailSender;

import static com.nemouw.database.DatabaseInit.initializeDB;
import static spark.Spark.*;

public class Main {

    public static void main(String[] args) {
        Config.validate(); // fail fast, before touching the DB or opening a port
        initializeDB();
        port(4567);

        AppModule.configure(DatabaseConnection.jdbi(),
                new AppModule.Secrets(Config.jwtSecret(), Config.otpHashSecret()),
                new DefaultEmailSender(Config.urlBase()), true);

        // Tells all bots to stay out of API subdomain entirely
        get("/robots.txt", (request, response) -> {
            response.type("text/plain");
            return "User-agent: *\nDisallow: /";
        });
    }
}