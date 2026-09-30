package com.nemouw.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure logic behind {@link GenerateEnums}: naming, parsing CHECK constraints and rendering Java source.
 * No database or third-party dependencies, so it is trivial to unit test.
 */
public final class EnumRenderer {
    private EnumRenderer() {}

    /** First line of every generated file. Used to recognise (and safely delete) stale generated files. */
    public static final String MARKER = "// GENERATED FILE - DO NOT EDIT.";

    public static final String REGENERATE_HINT =
            "mvn compile exec:java -Dexec.mainClass=com.nemouw.tools.GenerateEnums";

    /**
     * Explicit class names where the automatic "Table + Column" name is clumsy.
     * Key format: "table.column" for CHECK constraints, "table" for lookup tables.
     */
    public static final Map<String, String> NAME_OVERRIDES = Map.of(
            "user_verification_tokens.purpose", "VerificationPurpose",
            "specialist_profile.locale_code", "LocaleCode"
    );

    /** @param source human readable origin, e.g. "check constraint users.status" */
    public record EnumSpec(String className, String source, List<String> values) {}

    // Matches what Postgres stores for `col IN ('a','b')`:
    //   CHECK ((col = ANY (ARRAY['a'::text, 'b'::text])))
    // Anything else (ranges, regexes, NOT IN, multi-column rules) is deliberately NOT matched.
    private static final Pattern LITERAL = Pattern.compile("'((?:[^']|'')*)'::[a-z ]+");

    public static Optional<List<String>> parseAnyArray(String constraintDef, String column) {
        Pattern shape = Pattern.compile(
                "^CHECK \\(\\(\"?" + Pattern.quote(column) + "\"? = ANY \\(ARRAY\\[(.*)]\\)\\)\\)$");
        Matcher m = shape.matcher(constraintDef.trim());
        if (!m.matches()) return Optional.empty();

        List<String> values = new ArrayList<>();
        Matcher lit = LITERAL.matcher(m.group(1));
        while (lit.find()) values.add(lit.group(1).replace("''", "'"));
        return values.isEmpty() ? Optional.empty() : Optional.of(values);
    }

    // ---------- naming ----------

    public static String checkClassName(String table, String column) {
        String override = NAME_OVERRIDES.get(table + "." + column);
        if (override != null) return override;

        List<String> tableWords = new ArrayList<>(Arrays.asList(table.split("_")));
        int last = tableWords.size() - 1;
        tableWords.set(last, singular(tableWords.get(last)));
        List<String> colWords = Arrays.asList(column.split("_"));

        // user_identifier + identifier_type -> UserIdentifierType (drop the repeated words)
        int overlap = 0;
        for (int k = Math.min(tableWords.size(), colWords.size()); k > 0; k--) {
            if (tableWords.subList(tableWords.size() - k, tableWords.size()).equals(colWords.subList(0, k))) {
                overlap = k;
                break;
            }
        }
        List<String> words = new ArrayList<>(tableWords);
        words.addAll(colWords.subList(overlap, colWords.size()));
        return pascal(words);
    }

    public static String lookupClassName(String table) {
        String override = NAME_OVERRIDES.get(table);
        if (override != null) return override;
        List<String> words = new ArrayList<>(Arrays.asList(table.split("_")));
        int last = words.size() - 1;
        words.set(last, singular(words.get(last)));
        return pascal(words);
    }

    private static String singular(String w) {
        if (w.endsWith("ies")) return w.substring(0, w.length() - 3) + "y";
        if (w.endsWith("ss") || w.endsWith("us")) return w;   // "status", "address"
        if (w.endsWith("s")) return w.substring(0, w.length() - 1);
        return w;
    }

    private static String pascal(List<String> words) {
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1).toLowerCase(Locale.ROOT));
        }
        return sb.toString();
    }

    public static String constantName(String value) {
        String c = value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "");
        if (c.isEmpty()) throw new IllegalStateException("Cannot derive an enum constant from value '" + value + "'");
        return Character.isDigit(c.charAt(0)) ? "_" + c : c;
    }

    // ---------- rendering ----------

    public static String render(EnumSpec spec, String pkg) {
        Set<String> seen = new HashSet<>();
        StringBuilder consts = new StringBuilder();
        for (int i = 0; i < spec.values().size(); i++) {
            String value = spec.values().get(i);
            String name = constantName(value);
            if (!seen.add(name)) {
                throw new IllegalStateException(spec.className() + ": two values map to constant " + name);
            }
            String end = i == spec.values().size() - 1 ? ";" : ",";
            consts.append("    ").append(name).append("(\"").append(escape(value)).append("\")").append(end).append('\n');
        }

        String n = spec.className();
        return MARKER + "\n"
                + "// Source: " + spec.source() + "\n"
                + "// Regenerate: " + REGENERATE_HINT + "\n"
                + "package " + pkg + ";\n"
                + "\n"
                + "import com.fasterxml.jackson.annotation.JsonValue;\n"
                + "\n"
                + "import java.util.Optional;\n"
                + "\n"
                + "public enum " + n + " {\n"
                + consts
                + "\n"
                + "    private final String code;\n"
                + "\n"
                + "    " + n + "(String code) {\n"
                + "        this.code = code;\n"
                + "    }\n"
                + "\n"
                + "    /** The exact value stored in the database (and used in JSON). */\n"
                + "    @JsonValue\n"
                + "    public String code() {\n"
                + "        return code;\n"
                + "    }\n"
                + "\n"
                + "    /** Lookup by database value; empty if {@code code} is null or unknown. */\n"
                + "    public static Optional<" + n + "> tryFrom(String code) {\n"
                + "        for (" + n + " v : values()) {\n"
                + "            if (v.code.equals(code)) return Optional.of(v);\n"
                + "        }\n"
                + "        return Optional.empty();\n"
                + "    }\n"
                + "\n"
                + "    /** Lookup by database value; throws if unknown. */\n"
                + "    public static " + n + " fromCode(String code) {\n"
                + "        return tryFrom(code).orElseThrow(() ->\n"
                + "                new IllegalArgumentException(\"Unknown " + n + " code: \" + code));\n"
                + "    }\n"
                + "}\n";
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
