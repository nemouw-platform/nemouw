package com.nemouw.common.query;

import com.nemouw.common.enums.Codes;
import com.nemouw.common.enums.Coded;

import java.util.Set;

/** One filterable and sortable field: public name, type and the SQL expression behind it. */
public record Field(String name, FieldType type, String sql, Set<String> allowed) {
    public Field(String name, FieldType type, String sql) {
        this(name, type, sql, Set.of());
    }

    /** A text column with a fixed set of values, validated before it reaches SQL. */
    public static Field enumOf(String name, String sql, String... values) {
        return new Field(name, FieldType.ENUM, sql, Set.of(values));
    }

    /** An enum column: the allowed values are the enum's codes, so Java, API and filters cannot drift. */
    public static <E extends Enum<E> & Coded> Field enumOf(String name, String sql, Class<E> type) {
        return new Field(name, FieldType.ENUM, sql, Set.copyOf(Codes.all(type)));
    }

    Object parse(String raw) {
        Object value = type.parse(raw);
        if (type == FieldType.ENUM) {
            if (!(value instanceof String s) || !allowed.contains(s)) {
                throw new IllegalArgumentException("must be one of " + String.join(", ", allowed.stream().sorted().toList()));
            }
        }
        return value;
    }
}
