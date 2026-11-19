package com.nemouw.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Gender implements Coded {
    MALE("male"),
    FEMALE("female");

    private final String code;

    Gender(String code) {
        this.code = code;
    }

    @Override
    @JsonValue
    public String code() {
        return code;
    }
}
