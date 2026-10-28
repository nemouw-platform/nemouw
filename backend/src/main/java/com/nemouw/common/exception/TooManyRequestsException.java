package com.nemouw.common.exception;

import org.springframework.http.HttpHeaders;

/** 429 that tells the client when to try again, through the Retry-After header. */
public class TooManyRequestsException extends ApiException {
    private final int retryAfterSeconds;

    TooManyRequestsException(int retryAfterSeconds) {
        super(ErrorCode.TOO_MANY_REQUESTS);
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