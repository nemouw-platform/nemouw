package com.nemouw.common.exception;

import org.springframework.http.HttpHeaders;

/** An error that tells the client when to try again, through the Retry-After header. */
public class RetryAfterException extends ApiException {
    private final int retryAfterSeconds;

    RetryAfterException(ErrorCode errorCode, int retryAfterSeconds) {
        super(errorCode);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public int retryAfterSeconds() {
        return retryAfterSeconds;
    }

    @Override
    public HttpHeaders responseHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        return headers;
    }
}