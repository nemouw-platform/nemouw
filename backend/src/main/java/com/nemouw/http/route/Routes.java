package com.nemouw.http.route;

import com.nemouw.http.Json;
import spark.Request;
import spark.Response;
import spark.Route;

public final class Routes {
    private Routes() {}

    @FunctionalInterface
    public interface JsonRoute {
        Object handle(Request req, Response res) throws Exception;
    }

    public static Route json(int successStatus, JsonRoute route) {
        return (req, res) -> {
            res.type("application/json");
            Object body = route.handle(req, res);
            res.status(successStatus);
            return body == null ? "" : Json.write(body);
        };
    }
}