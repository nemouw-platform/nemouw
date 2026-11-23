package com.nemouw.user.management;

import com.nemouw.common.enums.UserRole;
import com.nemouw.common.enums.UserStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.core.simple.JdbcClient.StatementSpec;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Repository
class ManagedUserRepository {
    /** Null fields mean "any". {@code search} is raw text; LIKE wildcards are escaped here. */
    record Filter(UserRole role, UserStatus status, String search) {
    }

    private static final Set<String> UPDATABLE = Set.of("first_name", "last_name", "country_id", "preferred_language");

    private static final String FROM = """
            FROM users u
            JOIN user_roles r ON r.id = u.role_id
            JOIN user_identifiers ui ON ui.user_id = u.id AND ui.identifier_type = 'email' AND ui.is_primary
            """;

    private static final String SELECT = """
            SELECT u.id, u.first_name, u.last_name, ui.identifier AS email, r.code AS role_code, u.status,
                   u.country_id, u.preferred_language, u.created_at, u.last_login_at, u.deleted_at
            """ + FROM;

    private static final String MATCHES = """
            WHERE (CAST(:role AS text) IS NULL OR r.code = :role)
              AND (CAST(:status AS text) IS NULL OR u.status = :status)
              AND (CAST(:search AS text) IS NULL
                   OR search_normalize(u.first_name || ' ' || u.last_name) LIKE '%' || search_normalize(:search) || '%' ESCAPE '\\'
                   OR search_normalize(ui.identifier) LIKE '%' || search_normalize(:search) || '%' ESCAPE '\\')
            """;

    private final JdbcClient jdbc;

    ManagedUserRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    Optional<ManagedUserRow> findById(UUID id) {
        return jdbc.sql(SELECT + "WHERE u.id = :id")
                .param("id", id)
                .query(ManagedUserRow.class).optional();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    Optional<ManagedUserRow> lockById(UUID id) {
        return jdbc.sql(SELECT + "WHERE u.id = :id FOR UPDATE OF u")
                .param("id", id)
                .query(ManagedUserRow.class).optional();
    }

    List<ManagedUserRow> findByRole(UserRole role) {
        return jdbc.sql(SELECT + "WHERE r.code = :role ORDER BY u.created_at, u.id")
                .param("role", role.code())
                .query(ManagedUserRow.class).list();
    }

    List<ManagedUserRow> search(Filter filter, int limit, int offset) {
        return bind(jdbc.sql(SELECT + MATCHES + "ORDER BY u.created_at DESC, u.id LIMIT :limit OFFSET :offset"), filter)
                .param("limit", limit)
                .param("offset", offset)
                .query(ManagedUserRow.class).list();
    }

    long count(Filter filter) {
        return bind(jdbc.sql("SELECT count(*) " + FROM + MATCHES), filter)
                .query(Long.class).single();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    void update(UUID id, Map<String, Object> changes) {
        if (changes.isEmpty()) return;
        StringJoiner set = new StringJoiner(", ");
        for (String column : changes.keySet()) {
            if (!UPDATABLE.contains(column)) throw new IllegalArgumentException("Not updatable: " + column);
            set.add(column + " = :" + column);
        }
        jdbc.sql("UPDATE users SET " + set + " WHERE id = :id")
                .params(changes)
                .param("id", id)
                .update();
    }

    private static StatementSpec bind(StatementSpec sql, Filter filter) {
        return sql.param("role", filter.role() == null ? null : filter.role().code())
                .param("status", filter.status() == null ? null : filter.status().code())
                .param("search", filter.search() == null ? null : escapeLike(filter.search()));
    }

    /** Makes %, _ and \ in the admin's text match literally. */
    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}