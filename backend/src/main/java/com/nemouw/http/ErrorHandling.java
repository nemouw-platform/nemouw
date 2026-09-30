package com.nemouw.http;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import spark.Request;
import spark.Response;

import java.util.List;
import java.util.UUID;

import static spark.Spark.*;

public final class ErrorHandling {
    private static final Logger log = LogManager.getLogger(ErrorHandling.class);

    private ErrorHandling() {}

    public record ErrorBody(int status, String code, String message, String traceId,
                            @JsonInclude(JsonInclude.Include.NON_EMPTY) List<ApiException.FieldError> errors) {}

    public static void install() {
        before((req, res) -> {
            String traceId = UUID.randomUUID().toString();
            req.attribute("trace_id", traceId);
            ThreadContext.put("trace_id", traceId);
        });
        afterAfter((req, res) -> ThreadContext.clearAll());

        exception(ApiException.class, (e, req, res) ->
                write(req, res, e.status(), e.code(), e.getMessage(), e.errors()));

        exception(Exception.class, (e, req, res) -> {
            log.error("Unhandled exception", e);
            write(req, res, 500, "INTERNAL_ERROR", "Something went wrong", List.of());
        });

        notFound((req, res) -> {
            write(req, res, 404, "NOT_FOUND", "Route not found", List.of());
            return res.body();
        });
    }

    private static void write(Request req, Response res, int status, String code, String message,
                              List<ApiException.FieldError> errors) {
        res.status(status);
        res.type("application/json");
        res.body(Json.write(new ErrorBody(status, code, message, req.attribute("trace_id"), errors)));
    }
}