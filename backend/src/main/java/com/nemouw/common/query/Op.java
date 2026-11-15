package com.nemouw.common.query;

import java.util.Arrays;
import java.util.Optional;

public enum Op {
    EQ, GT, GTE, LT, LTE, CONTAINS, IS_NULL;

    public String code() {
        return name().toLowerCase();
    }

    static Optional<Op> of(String code) {
        return Arrays.stream(values()).filter(o -> o.code().equals(code)).findFirst();
    }
}
