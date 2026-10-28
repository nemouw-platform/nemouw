package com.nemouw.security;

import com.nemouw.common.exception.Errors;
import com.nemouw.common.web.ClientIpResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Rejects clients that exceed the per-second budget with 429 and Retry-After.
 * Not a bean on purpose: SecurityConfig adds it to each security chain right after the CORS filter, so the 429
 * carries CORS headers and the browser lets the frontend read it. The shared counters live in {@link RateLimiter}.
 */
public class RateLimitFilter extends OncePerRequestFilter {
    private static final int RETRY_AFTER_SECONDS = 1;

    private final RateLimiter limiter;
    private final ClientIpResolver clientIp;
    private final SecurityErrorHandler errors;

    RateLimitFilter(RateLimiter limiter, ClientIpResolver clientIp, SecurityErrorHandler errors) {
        this.limiter = limiter;
        this.clientIp = clientIp;
        this.errors = errors;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod());   // CORS preflight
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String ip = clientIp.resolve(request);
        if (!limiter.tryAcquire(ip != null ? ip : "unknown")) {
            errors.write(response, Errors.tooManyRequests(RETRY_AFTER_SECONDS));
            return;
        }
        chain.doFilter(request, response);
    }
}