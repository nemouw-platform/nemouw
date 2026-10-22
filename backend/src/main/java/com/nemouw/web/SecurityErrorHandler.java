package com.nemouw.web;

import tools.jackson.databind.ObjectMapper;
import com.nemouw.error.ApiException;
import com.nemouw.error.ErrorBody;
import com.nemouw.error.Errors;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * Spring Security rejects requests in a servlet filter, before any controller runs, so
 * GlobalExceptionHandler never sees them. This writes the same JSON {@link ErrorBody} for those 401 and 403s.
 */
@Component
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final ObjectMapper mapper;

    public SecurityErrorHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /** 401. A token that was presented but rejected is INVALID_TOKEN; no token at all is UNAUTHENTICATED. */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e)
            throws IOException {
        write(response, e instanceof OAuth2AuthenticationException
                ? Errors.invalidAccessToken()
                : Errors.missingAccessToken());
    }

    /** 403: authenticated, but not allowed (e.g. a setup token calling a normal endpoint). */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e)
            throws IOException {
        write(response, Errors.forbidden());
    }

    private void write(HttpServletResponse response, ApiException error) throws IOException {
        response.setStatus(error.status());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), new ErrorBody(
                error.status(), error.code(), error.getMessage(), MDC.get(TraceIdFilter.MDC_KEY), List.of()));
    }
}