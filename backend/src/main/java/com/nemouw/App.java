package com.nemouw;

import static spark.Spark.*;

public class App {
    public static void main(String[] args) {
        get("/health", (req, res) -> "Nemouw backend is running!!!!");
    }
}