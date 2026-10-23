package com.nemouw.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * One log line per request when it finishes: method, path, status and duration. These become fields in the
 * JSON logs (and key=value pairs in the readable ones), so dashboards can chart traffic, errors and latency.
 * Sits just inside TraceIdFilter and outside Spring Security, so rejected requests (401, 403, 429) are logged too.
 * Only the path is logged, never the query string, in case a token ever travels there.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestLogFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestLogFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long start = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            int status = response.getStatus();
            var event = status >= 500 ? log.atWarn() : log.atInfo();
            event.addKeyValue("method", request.getMethod())
                    .addKeyValue("path", request.getRequestURI())
                    .addKeyValue("status", status)
                    .addKeyValue("duration_ms", (System.nanoTime() - start) / 1_000_000)
                    .log("http_request");
        }
    }
}