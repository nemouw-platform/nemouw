package com.nemouw.http;

import spark.Request;
import spark.Response;
import spark.Route;

public final class JsonRoutes {
    private JsonRoutes() {}

    @FunctionalInterface
    public interface JsonRoute {
        Object handle(Request req, Response res) throws Exception;
    }

    public static Route json(int successStatus, JsonRoute route) {
        return (req, res) -> {
            Object body = route.handle(req, res);
            res.status(successStatus);
            if (body == null) return "";
            res.type("application/json");
            return Json.write(body);
        };
    }

    public static Route ok(JsonRoute r)        { return json(200, r); }
    public static Route created(JsonRoute r)   { return json(201, r); }
    public static Route accepted(JsonRoute r)  { return json(202, r); }
    public static Route noContent(JsonRoute r) { return json(204, r); }
}