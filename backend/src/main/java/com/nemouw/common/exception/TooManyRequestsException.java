package com.nemouw.common.exception;

/** 429 that tells the client when to try again. */
public class TooManyRequestsException extends RetryAfterException {
    TooManyRequestsException(int retryAfterSeconds) {
        super(ErrorCode.TOO_MANY_REQUESTS, retryAfterSeconds);
    }
}