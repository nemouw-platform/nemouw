package com.nemouw.common.query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;

import static com.nemouw.common.query.Op.*;

/** What a field holds: decides the operators it accepts and how its values are parsed. */
public enum FieldType {
    UUID(Set.of(EQ, IS_NULL)),
    TEXT(Set.of(EQ, CONTAINS, IS_NULL)),
    ENUM(Set.of(EQ, IS_NULL)),
    NUMBER(Set.of(EQ, GT, GTE, LT, LTE, IS_NULL)),
    DATE(Set.of(EQ, GT, GTE, LT, LTE, IS_NULL)),
    TIMESTAMP(Set.of(GT, GTE, LT, LTE, IS_NULL)),
    BOOLEAN(Set.of(EQ));

    final Set<Op> ops;

    FieldType(Set<Op> ops) {
        this.ops = ops;
    }

    /** Throws IllegalArgumentException (or a subclass) when the text is not a valid value. */
    Object parse(String raw) {
        String v = raw.trim();
        return switch (this) {
            case UUID -> java.util.UUID.fromString(v);
            case TEXT, ENUM -> {
                if (v.isEmpty() || v.length() > 100) throw new IllegalArgumentException("must be 1 to 100 characters");
                yield v;
            }
            case NUMBER -> new BigDecimal(v);
            case DATE -> LocalDate.parse(v);
            case TIMESTAMP -> v.length() == 10 ? LocalDate.parse(v).atStartOfDay().atOffset(ZoneOffset.UTC)
                    : OffsetDateTime.parse(v);
            case BOOLEAN -> {
                if (!v.equalsIgnoreCase("true") && !v.equalsIgnoreCase("false")) {
                    throw new IllegalArgumentException("must be true or false");
                }
                yield Boolean.parseBoolean(v);
            }
        };
    }
}
