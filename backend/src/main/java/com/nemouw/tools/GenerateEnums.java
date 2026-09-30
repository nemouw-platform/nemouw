package com.nemouw.tools;

import com.nemouw.database.DatabaseConnection;
import com.nemouw.database.DatabaseInit;
import com.nemouw.tools.EnumRenderer.EnumSpec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Generates Java enums from the live database schema so the code can never drift from it.
 *
 * <pre>
 *   mvn compile exec:java -Dexec.mainClass=com.nemouw.tools.GenerateEnums                   # (re)generate
 *   mvn compile exec:java -Dexec.mainClass=com.nemouw.tools.GenerateEnums -Dexec.args=--check  # CI: fail on drift
 * </pre>
 *
 * Two kinds of enums are produced:
 * <ul>
 *   <li><b>Lookup tables</b> listed in {@link #LOOKUP_TABLES}: one constant per row's {@code code}.</li>
 *   <li><b>CHECK constraints</b> of the form {@code col IN ('a','b',...)}: one constant per listed value.</li>
 * </ul>
 * Pending Flyway migrations are applied first, so the output always reflects the latest schema.
 */
public final class GenerateEnums {
    private GenerateEnums() {}

    static final Path OUTPUT_DIR = Path.of("src/main/java/com/nemouw/enums");
    static final String PACKAGE = "com.nemouw.enums";

    /**
     * Tables whose rows ARE the enum (must have a unique text {@code code} column).
     * Only add tables whose rows are fixed by migrations. Data that admins can edit at runtime
     * (specialties, conditions, ...) must NOT be here: a new row would have no matching constant.
     */
    static final List<String> LOOKUP_TABLES = List.of("user_roles");

    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[a-z][a-z0-9_]*");

    public static void main(String[] args) {
        boolean check = args.length > 0 && args[0].equals("--check");
        try {
            if (!Files.isDirectory(Path.of("src/main/java"))) {
                throw new IllegalStateException("Run this from the project root (src/main/java not found in "
                        + Path.of("").toAbsolutePath() + ")");
            }
            DatabaseInit.initializeDB();

            List<EnumSpec> specs;
            try (Connection c = DatabaseConnection.getConnection()) {
                specs = new ArrayList<>(readLookupEnums(c));
                specs.addAll(readCheckEnums(c));
            }

            Map<String, String> files = new TreeMap<>();
            for (EnumSpec spec : specs) {
                String previous = files.put(spec.className() + ".java", EnumRenderer.render(spec, PACKAGE));
                if (previous != null) {
                    throw new IllegalStateException("Two enums resolve to the class name " + spec.className()
                            + ". Add an entry to EnumRenderer.NAME_OVERRIDES.");
                }
            }

            int problems = check ? verify(files) : write(files);
            System.exit(problems == 0 ? 0 : 1);
        } catch (Exception e) {
            System.err.println("Enum generation failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(2);   // also stops the Hikari pool threads
        }
    }

    // ---------- reading the schema ----------

    private static List<EnumSpec> readLookupEnums(Connection c) throws SQLException {
        List<EnumSpec> out = new ArrayList<>();
        for (String table : LOOKUP_TABLES) {
            if (!SAFE_IDENTIFIER.matcher(table).matches()) throw new IllegalStateException("Bad table name " + table);
            List<String> codes = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT code FROM " + table + " ORDER BY code");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) codes.add(rs.getString(1));
            }
            if (codes.isEmpty()) throw new IllegalStateException("Lookup table " + table + " has no rows");
            out.add(new EnumSpec(EnumRenderer.lookupClassName(table), "lookup table " + table, codes));
        }
        return out;
    }

    private static List<EnumSpec> readCheckEnums(Connection c) throws SQLException {
        String sql = """
                SELECT cl.relname, a.attname, pg_get_constraintdef(con.oid)
                FROM pg_constraint con
                JOIN pg_class cl ON cl.oid = con.conrelid
                JOIN pg_namespace ns ON ns.oid = cl.relnamespace
                JOIN pg_attribute a ON a.attrelid = cl.oid AND a.attnum = con.conkey[1]
                WHERE con.contype = 'c'
                  AND ns.nspname = current_schema()
                  AND array_length(con.conkey, 1) = 1
                ORDER BY cl.relname, a.attname, con.conname""";

        List<EnumSpec> out = new ArrayList<>();
        Set<String> seenColumns = new TreeSet<>();
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String table = rs.getString(1), column = rs.getString(2), def = rs.getString(3);
                var values = EnumRenderer.parseAnyArray(def, column);
                if (values.isEmpty()) continue;   // range / regex / NOT IN checks are not enums
                if (!seenColumns.add(table + "." + column)) {
                    throw new IllegalStateException(table + "." + column + " has more than one IN-list CHECK");
                }
                out.add(new EnumSpec(EnumRenderer.checkClassName(table, column),
                        "check constraint " + table + "." + column, values.get()));
            }
        }
        return out;
    }

    // ---------- writing / verifying ----------

    private static int write(Map<String, String> files) throws IOException {
        Files.createDirectories(OUTPUT_DIR);
        for (Map.Entry<String, String> f : files.entrySet()) {
            Path p = OUTPUT_DIR.resolve(f.getKey());
            if (!Files.exists(p) || !normalize(Files.readString(p)).equals(f.getValue())) {
                Files.writeString(p, f.getValue(), StandardCharsets.UTF_8);
                System.out.println("wrote    " + p);
            } else {
                System.out.println("unchanged " + p);
            }
        }
        for (Path stale : staleGeneratedFiles(files)) {
            Files.delete(stale);
            System.out.println("deleted  " + stale + " (no longer in the database)");
        }
        return 0;
    }

    private static int verify(Map<String, String> files) throws IOException {
        int problems = 0;
        for (Map.Entry<String, String> f : files.entrySet()) {
            Path p = OUTPUT_DIR.resolve(f.getKey());
            if (!Files.exists(p)) {
                System.err.println("MISSING  " + p);
                problems++;
            } else if (!normalize(Files.readString(p)).equals(f.getValue())) {
                System.err.println("OUT OF DATE  " + p);
                problems++;
            }
        }
        for (Path stale : staleGeneratedFiles(files)) {
            System.err.println("STALE    " + stale);
            problems++;
        }
        if (problems > 0) System.err.println("Enums are out of sync with the database. Run: " + EnumRenderer.REGENERATE_HINT);
        else System.out.println("Enums match the database.");
        return problems;
    }

    /** Files in the output dir that we generated earlier but that the schema no longer produces. */
    private static List<Path> staleGeneratedFiles(Map<String, String> files) throws IOException {
        List<Path> stale = new ArrayList<>();
        if (!Files.isDirectory(OUTPUT_DIR)) return stale;
        try (Stream<Path> s = Files.list(OUTPUT_DIR)) {
            for (Path p : (Iterable<Path>) s::iterator) {
                if (!p.getFileName().toString().endsWith(".java") || files.containsKey(p.getFileName().toString())) continue;
                if (Files.readString(p).startsWith(EnumRenderer.MARKER)) stale.add(p);   // never touches hand-written files
            }
        }
        return stale;
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");   // tolerate CRLF checkouts on Windows
    }
}
