package com.nemouw.common.query;

import java.util.List;
import java.util.Optional;

/** The fields one resource exposes, plus sort keys it computes itself (for example "distance"). */
public record FieldSet(List<Field> fields, List<String> computedSorts) {
    public record FieldInfo(String name, String type, List<String> operators, List<String> values) {
    }

    public static FieldSet of(Field... fields) {
        return new FieldSet(List.of(fields), List.of());
    }

    public FieldSet withComputedSorts(String... keys) {
        return new FieldSet(fields, List.of(keys));
    }

    public Optional<Field> find(String name) {
        return fields.stream().filter(f -> f.name().equals(name)).findFirst();
    }

    /** What a client needs to build a filter UI. */
    public List<FieldInfo> describe() {
        return fields.stream()
                .map(f -> new FieldInfo(f.name(), f.type().name().toLowerCase(),
                        f.type().ops.stream().map(Op::code).sorted().toList(),
                        f.allowed().stream().sorted().toList()))
                .toList();
    }
}
