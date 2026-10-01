package com.nemouw.middleware;

import com.nemouw.error.ApiException;

import static spark.Spark.before;

/**
 * Cookie auth needs this. Browsers always send an Origin header on cross-origin POST/PUT/PATCH/DELETE,
 * so a request from another site is rejected here. Requests without Origin (curl, server-to-server)
 * are not a CSRF risk: only a browser can be tricked into attaching someone's cookies.
 */
public final class CsrfGuard {
    private CsrfGuard() {}

    public static void install() {
        before((req, res) -> {
            String m = req.requestMethod();
            if (m.equals("GET") || m.equals("HEAD") || m.equals("OPTIONS")) return;
            String origin = req.headers("Origin");
            if (origin != null && !CORSFilter.isAllowed(origin)) {
                throw ApiException.forbidden("CSRF_BLOCKED", "Origin not allowed");
            }
        });
    }
}