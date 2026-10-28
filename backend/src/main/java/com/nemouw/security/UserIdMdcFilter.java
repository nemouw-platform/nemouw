package com.nemouw.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Adds user_id to the log context once the caller is authenticated, so every log line of that request carries it
 * (a JSON field in structured logs). TraceIdFilter clears the context when the request ends.
 * Not a bean: SecurityConfig adds it to the API chain, right after the token has been checked.
 */
public class UserIdMdcFilter extends OncePerRequestFilter {
    public static final String MDC_KEY = "user_id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            MDC.put(MDC_KEY, authentication.getName());   // the token's subject, which is the user id
        }
        chain.doFilter(request, response);
    }
}