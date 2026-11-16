package com.nemouw.common.query;

import java.util.List;

/** A validated list request: filters, sort keys and paging. Empty sorts means "resource default". */
public record QuerySpec(List<Condition> filters, List<SortKey> sorts, int page, int pageSize) {
    public record Condition(Field field, Op op, List<Object> values) {
    }

    public record SortKey(String key, boolean desc) {
    }

    public int offset() {
        return (page - 1) * pageSize;
    }
}
