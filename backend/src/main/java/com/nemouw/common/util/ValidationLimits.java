package com.nemouw.common.util;

/** Length limits shared by request validation (characters, after trimming). */
public final class ValidationLimits {
    private ValidationLimits() {}

    // 254 is the longest valid email address (RFC 5321)
    public static final int EMAIL_MAX_LENGTH = 254;
    public static final int NAME_MAX_LENGTH = 100;
    public static final int TEXT_MAX = 100;
    public static final int DESCRIPTION_MAX = 1000;
    public static final int HEADLINE_MAX = 150;
    public static final int BIO_MAX = 2000;
}