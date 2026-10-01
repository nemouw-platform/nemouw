package com.nemouw.middleware;

import com.nemouw.enums.UserRole;
import com.nemouw.http.AuthCookies;
import com.nemouw.error.Errors;
import com.nemouw.repository.SessionRepository;
import com.nemouw.security.TokenService;
import org.apache.logging.log4j.ThreadContext;
import org.jdbi.v3.core.Jdbi;
import spark.Request;
import spark.Response;

import java.util.List;
import java.util.regex.Pattern;

import static spark.Spark.before;

/**
 * Deny by default: everything under /v1 needs a valid session unless it matches PUBLIC.
 * PUBLIC mirrors the `security: []` operations in the OpenAPI spec.
 */
public class AuthMiddleware {

    private record Rule(String method, Pattern path) {}

    private static Rule rule(String method, String regex) {
        return new Rule(method, Pattern.compile(regex));
    }

    // "me" and "by-id" are excluded from the :username routes so they can never be reached anonymously
    private static final List<Rule> PUBLIC = List.of(
            rule("POST", "^/v1/auth/.+$"),
            rule("GET", "^/v1/specialties$"),
            rule("GET", "^/v1/conditions$"),
            rule("GET", "^/v1/countries(/[^/]+)?$"),
            rule("GET", "^/v1/syndicates(/[^/]+)?$"),
            rule("GET", "^/v1/specialists$"),
            rule("GET", "^/v1/specialists/(?!me$|by-id$)[^/]+$"),
            rule("POST", "^/v1/specialists/(?!me$|by-id$)[^/]+/events$")
    );

    private final Jdbi jdbi;
    private final TokenService tokens;
    private final SessionRepository sessions;

    public AuthMiddleware(Jdbi jdbi, TokenService tokens, SessionRepository sessions) {
        this.jdbi = jdbi;
        this.tokens = tokens;
        this.sessions = sessions;
    }

    /** Register AFTER ErrorHandling, CORS, the DDoS shield and CsrfGuard, and BEFORE any routes. */
    public void install() {
        before("/v1/*", this::authenticate);
    }

    private void authenticate(Request req, Response res) {
        String method = req.requestMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) return;      // CORS preflight carries no cookies

        // req.uri() is the raw path Spark itself routes on, so the allowlist and the router agree
        String path = req.uri();
        if (path.length() > 1 && path.endsWith("/")) path = path.substring(0, path.length() - 1);
        for (Rule r : PUBLIC) {
            if (r.method().equalsIgnoreCase(method) && r.path().matcher(path).matches()) return;
        }

        String token = req.cookie(AuthCookies.ACCESS);
        if (token == null || token.isBlank()) throw Errors.missingAccessToken();

        var claims = tokens.verifyAccessToken(token);

        var session = jdbi.withHandle(h -> sessions.findActive(h, claims.sessionId(), claims.userId()))
                .orElseThrow(Errors::invalidAccessToken);

        req.attribute(AuthContext.ATTR,
                new AuthContext(claims.userId(), claims.sessionId(), UserRole.fromCode(session.roleCode())));
        ThreadContext.put("user_id", claims.userId().toString());   // optional: add %X{user_id} to log4j2.xml
    }
}