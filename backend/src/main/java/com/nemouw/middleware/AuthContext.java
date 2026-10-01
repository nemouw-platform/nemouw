package com.nemouw.middleware;

import com.nemouw.enums.UserRole;
import com.nemouw.error.Errors;
import spark.Request;

import java.util.UUID;

public record AuthContext(UUID userId, UUID sessionId, UserRole role) {
    public static final String ATTR = "auth_context";

    // Call at the top of a controller method. Throws 401 if the route was somehow not protected.
    public static AuthContext of(Request req) {
        AuthContext ctx = req.attribute(ATTR);
        if (ctx == null) throw Errors.missingAccessToken();
        return ctx;
    }

    // Coarse role gate. Finer rules ("same syndicate", "own profile") belong in the service.
    public void requireRole(UserRole... allowed) {
        for (UserRole r : allowed) if (r == role) return;
        throw Errors.forbidden();
    }
}