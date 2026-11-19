package com.nemouw.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/** Currencies a specialist can price sessions in. Adding one is a new constant and a new CHECK value. */
public enum Currency implements Coded {
    USD("USD");

    public static final Currency DEFAULT = USD;

    private final String code;

    Currency(String code) {
        this.code = code;
    }

    @Override
    @JsonValue
    public String code() {
        return code;
    }

    /** The currency a country's specialists start with: the country's own when supported, else the default. */
    public static Currency forCountry(String countryCurrencyCode) {
        return Codes.tryFrom(Currency.class, countryCurrencyCode).orElse(DEFAULT);
    }
}
