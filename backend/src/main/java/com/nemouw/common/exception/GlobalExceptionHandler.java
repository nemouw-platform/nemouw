package com.nemouw.common.exception;

import com.nemouw.common.web.TraceIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Locale;

/**
 * Turns every exception into the same JSON {@link ErrorBody}.
 * Spring Security part: rethrow AuthenticationException and AccessDeniedException in
 * {@link #handleUnexpected}, otherwise they become 500s instead of 401/403.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorBody> handleApi(ApiException e) {
        if (e.status() >= 500) {
            log.error("api_error code={} status={}", e.code(), e.status(), e);
        } else {
            log.info("api_error code={} status={}", e.code(), e.status());
        }
        return respond(e.status(), e.code(), e.getMessage(), e.fieldErrors(), e.responseHeaders());
    }

    /** Failed @Valid (also covers MethodArgumentNotValidException). */
    @ExceptionHandler(BindException.class)
    ResponseEntity<ErrorBody> handleBinding(BindException e) {
        List<FieldError> errors = e.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::toFieldError)
                .toList();
        return handleApi(new ValidationException(errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorBody> handleUnreadableBody(HttpMessageNotReadableException e) {
        boolean missing = e.getMessage() != null && e.getMessage().startsWith("Required request body is missing");
        return handleApi(Errors.malformedJson(missing ? "Request body is required" : "Request body is not valid JSON"));
    }

    @ExceptionHandler(TypeMismatchException.class)
    ResponseEntity<ErrorBody> handleTypeMismatch(TypeMismatchException e) {
        String name = e instanceof MethodArgumentTypeMismatchException m ? m.getName() : e.getPropertyName();
        String message = name != null ? "Invalid value for '" + name + "'" : ErrorCode.BAD_REQUEST.defaultMessage();
        return handleApi(new ApiException(ErrorCode.BAD_REQUEST, message));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorBody> handleUnexpected(Exception e) throws Exception {
        // Not ours: let Spring Security's entry point / access-denied handler answer with 401 / 403
        if (e instanceof AuthenticationException || e instanceof AccessDeniedException) {
            throw e;
        }

        // Spring's web exceptions (unknown route, wrong method, bad media type...)
        // implement ErrorResponse and keep their own status.
        if (e instanceof ErrorResponse framework) {
            int status = framework.getStatusCode().value();
            if (status == ErrorCode.NOT_FOUND.status()) {
                return handleApi(new ApiException(ErrorCode.NOT_FOUND));
            }
            if (status >= 500) {
                log.error("Server error raised by the framework", e);
            }
            HttpStatus known = HttpStatus.resolve(status);
            String code = known != null ? known.name() : "HTTP_ERROR";
            String message = known != null ? known.getReasonPhrase() : "Request failed";
            return respond(status, code, message, List.of(), framework.getHeaders());
        }

        log.error("Unhandled exception", e);
        ErrorCode internal = ErrorCode.INTERNAL_ERROR;
        return respond(internal.status(), internal.name(), internal.defaultMessage(), List.of(), HttpHeaders.EMPTY);
    }

    private static ResponseEntity<ErrorBody> respond(int status, String code, String message,
                                                     List<FieldError> errors, HttpHeaders headers) {
        ErrorBody body = new ErrorBody(status, code, message, MDC.get(TraceIdFilter.MDC_KEY), errors);
        return ResponseEntity.status(status)
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    private static FieldError toFieldError(org.springframework.validation.FieldError fe) {
        String field = toSnakeCase(fe.getField());
        String constraint = fe.getCode() == null ? "" : fe.getCode();
        return switch (constraint) {
            case "NotNull", "NotBlank", "NotEmpty" -> FieldErrors.required(field);
            case "Email" -> FieldErrors.mustBeEmail(field);
            default -> FieldErrors.invalidFormat(field,
                    fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "is invalid");
        };
    }

    private static String toSnakeCase(String camel) {
        return camel.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }
}