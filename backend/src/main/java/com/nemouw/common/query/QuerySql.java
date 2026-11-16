package com.nemouw.common.query;

import com.nemouw.common.query.QuerySpec.Condition;
import com.nemouw.common.query.QuerySpec.SortKey;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Turns a {@link QuerySpec} into SQL. Identifiers come only from {@link FieldSet}; every value is bound.
 * {@link #where} and {@link #order} are the building blocks, {@link #list} is a ready statement for plain tables.
 */
public final class QuerySql {
    public record Statement(String dataSql, String countSql, Map<String, Object> params) {
    }

    private QuerySql() {
    }

    /** One predicate per filter. Bound values go into {@code params} as {@code prefix0, prefix1, ...}. */
    public static List<String> where(List<Condition> filters, String prefix, Map<String, Object> params) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < filters.size(); i++) {
            Condition c = filters.get(i);
            String name = prefix + i;
            out.add(predicate(c, name));
            if (c.op() != Op.IS_NULL) params.put(name, boundValue(c));
        }
        return out;
    }

    public static String order(Field f, boolean desc) {
        String sql = f.type() == FieldType.TEXT ? "lower(" + f.sql() + ")" : f.sql();
        return sql + (desc ? " DESC" : " ASC") + " NULLS LAST";
    }

    /**
     * A complete page and count statement. {@code baseWhere} is the resource's own restriction (use "TRUE" for none);
     * filters can only narrow it. {@code defaultOrder} applies when the client sent no sort; {@code tieBreaker}
     * keeps pages stable.
     */
    public static Statement list(String select, String from, String baseWhere, QuerySpec spec, FieldSet fields,
                                 String defaultOrder, String tieBreaker) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("limit", spec.pageSize());
        params.put("offset", spec.offset());
        List<String> conditions = new ArrayList<>(List.of("(" + baseWhere + ")"));
        conditions.addAll(where(spec.filters(), "ff", params));

        List<String> order = new ArrayList<>();
        for (SortKey s : spec.sorts()) order.add(order(fields.find(s.key()).orElseThrow(), s.desc()));
        if (order.isEmpty()) order.add(defaultOrder);
        order.add(tieBreaker);

        String tail = "FROM " + from + "\nWHERE " + String.join("\n  AND ", conditions);
        return new Statement("SELECT " + select + "\n" + tail + "\nORDER BY " + String.join(", ", order)
                + "\nLIMIT :limit OFFSET :offset", "SELECT count(*)\n" + tail, Map.copyOf(params));
    }

    private static String predicate(Condition c, String param) {
        String sql = c.field().sql();
        boolean text = c.field().type() == FieldType.TEXT;
        return switch (c.op()) {
            case EQ -> (text ? "lower(" + sql + ")" : sql) + " IN (:" + param + ")";
            case GT -> sql + " > :" + param;
            case GTE -> sql + " >= :" + param;
            case LT -> sql + " < :" + param;
            case LTE -> sql + " <= :" + param;
            case CONTAINS -> "search_normalize(" + sql + ") LIKE '%' || search_normalize(:" + param + ") || '%' ESCAPE '\\'";
            case IS_NULL -> sql + ((Boolean) c.values().getFirst() ? " IS NULL" : " IS NOT NULL");
        };
    }

    private static Object boundValue(Condition c) {
        boolean text = c.field().type() == FieldType.TEXT;
        return switch (c.op()) {
            case EQ -> text ? c.values().stream().map(v -> ((String) v).toLowerCase(Locale.ROOT)).toList() : c.values();
            case CONTAINS -> ((String) c.values().getFirst()).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            default -> c.values().getFirst();
        };
    }
}
