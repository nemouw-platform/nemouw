package com.nemouw.common.query;

import com.nemouw.common.exception.FieldErrors;
import com.nemouw.common.query.QuerySpec.Condition;
import com.nemouw.common.query.QuerySpec.SortKey;
import com.nemouw.common.util.Validator;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads filter[...], sort, page and page_size from query parameters. Bad input is reported on the caller's
 * {@link Validator}, so a resource can add its own checks and fail once with every problem.
 */
public final class QueryParser {
    public static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_VALUES = 10;
    private static final int MAX_FILTERS = 20;
    private static final int MAX_SORTS = 3;
    private static final Pattern FILTER_KEY = Pattern.compile("^filter\\[([a-z_]+)](?:\\[([a-z_]+)])?$");

    private final FieldSet fields;
    private final MultiValueMap<String, String> params;
    private final Validator v;

    private QueryParser(FieldSet fields, MultiValueMap<String, String> params, Validator v) {
        this.fields = fields;
        this.params = params;
        this.v = v;
    }

    public static QuerySpec parse(FieldSet fields, MultiValueMap<String, String> params, Validator v, int maxPageSize) {
        QueryParser p = new QueryParser(fields, params, v);
        Integer page = integer(params, "page", 1, 10_000, v);
        Integer size = integer(params, "page_size", 1, maxPageSize, v);
        return new QuerySpec(p.filters(), p.sorts(), page == null ? 1 : page, size == null ? DEFAULT_PAGE_SIZE : size);
    }

    /** A whole number parameter within [min, max], or null when absent or invalid (the error is recorded). */
    public static Integer integer(MultiValueMap<String, String> params, String name, int min, int max, Validator v) {
        String value = params.getFirst(name);
        if (value == null || value.isBlank()) return null;
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed >= min && parsed <= max) return parsed;
        } catch (NumberFormatException ignored) {
            // reported below
        }
        fail(v, name, "must be a whole number from " + min + " to " + max);
        return null;
    }

    private List<SortKey> sorts() {
        String raw = params.getFirst("sort");
        if (raw == null || raw.isBlank()) return List.of();
        List<SortKey> out = new ArrayList<>();
        for (String part : raw.split(",")) {
            String token = part.trim();
            boolean desc = token.startsWith("-");
            String key = desc ? token.substring(1) : token;
            boolean computed = fields.computedSorts().contains(key);
            if (token.isEmpty() || (computed ? desc : fields.find(key).isEmpty())) {
                fail(v, "sort", "cannot sort by " + token);
            } else if (out.stream().noneMatch(s -> s.key().equals(key))) {
                out.add(new SortKey(key, desc));
            }
        }
        if (out.size() > MAX_SORTS) fail(v, "sort", "at most " + MAX_SORTS + " keys");
        return out;
    }

    private List<Condition> filters() {
        List<Condition> out = new ArrayList<>();
        params.forEach((key, values) -> {
            if (key.startsWith("filter[")) filter(key, values).ifPresent(out::add);
        });
        if (out.size() > MAX_FILTERS) fail(v, "filter", "at most " + MAX_FILTERS + " filters");
        return out;
    }

    private Optional<Condition> filter(String key, List<String> raw) {
        Matcher m = FILTER_KEY.matcher(key);
        if (!m.matches()) return bad(key, "use filter[field] or filter[field][op]");
        Field field = fields.find(m.group(1)).orElse(null);
        if (field == null) return bad(key, "unknown field");
        Op op = m.group(2) == null ? Op.EQ : Op.of(m.group(2)).orElse(null);
        if (op == null) return bad(key, "unknown operator");
        if (!field.type().ops.contains(op)) return bad(key, op.code() + " does not apply to " + field.type());

        List<String> items = raw.stream()
                .flatMap(s -> op == Op.EQ ? Arrays.stream(s.split(",")) : Arrays.stream(new String[]{s}))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
        if (items.isEmpty()) return bad(key, "a value is required");
        if (items.size() > (op == Op.EQ ? MAX_VALUES : 1)) return bad(key, "too many values");
        try {
            List<Object> values = new ArrayList<>();
            for (String item : items) {
                values.add(op == Op.IS_NULL ? FieldType.BOOLEAN.parse(item) : field.parse(item));
            }
            return Optional.of(new Condition(field, op, values));
        } catch (RuntimeException e) {
            return bad(key, "invalid " + field.type().name().toLowerCase() + " value");
        }
    }

    private Optional<Condition> bad(String name, String message) {
        fail(v, name, message);
        return Optional.empty();
    }

    private static void fail(Validator v, String name, String message) {
        v.check(false, FieldErrors.invalidFormat(name, message));
    }
}
