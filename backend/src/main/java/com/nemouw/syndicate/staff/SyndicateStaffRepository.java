package com.nemouw.syndicate.staff;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class SyndicateStaffRepository {
    private static final String SELECT = """
            SELECT ss.id, ss.syndicate_id, ss.user_id, ss.title, ss.is_owner, ss.created_at
            FROM syndicate_staff ss
            """;

    private final JdbcClient jdbc;

    SyndicateStaffRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    UUID insertOwner(UUID syndicateId, UUID userId) {
        return insert(syndicateId, userId, null, true);
    }

    UUID insertMember(UUID syndicateId, UUID userId, String title) {
        return insert(syndicateId, userId, title, false);
    }

    /**
     * Both ids must match, so a staff id from another syndicate finds nothing.
     */
    Optional<SyndicateStaffRow> findInSyndicate(UUID staffId, UUID syndicateId) {
        return jdbc.sql(SELECT + "WHERE ss.id = :id AND ss.syndicate_id = :syndicateId")
                .param("id", staffId)
                .param("syndicateId", syndicateId)
                .query(SyndicateStaffRow.class)
                .optional();
    }

    Optional<SyndicateStaffRow> findOwner(UUID syndicateId) {
        return jdbc.sql(SELECT + "WHERE ss.syndicate_id = :syndicateId AND ss.is_owner")
                .param("syndicateId", syndicateId)
                .query(SyndicateStaffRow.class)
                .optional();
    }

    List<SyndicateStaffRow> findBySyndicate(UUID syndicateId) {
        return jdbc.sql(SELECT + "WHERE ss.syndicate_id = :syndicateId ORDER BY ss.created_at")
                .param("syndicateId", syndicateId)
                .query(SyndicateStaffRow.class)
                .list();
    }

    /**
     * False when no staff member with this id exists in this syndicate.
     */
    boolean updateTitle(UUID staffId, UUID syndicateId, String title) {
        return jdbc.sql("UPDATE syndicate_staff SET title = :title WHERE id = :id AND syndicate_id = :syndicateId")
                .param("title", title)
                .param("id", staffId)
                .param("syndicateId", syndicateId)
                .update() > 0;
    }

    void delete(UUID staffId) {
        jdbc.sql("DELETE FROM syndicate_staff WHERE id = :id")
                .param("id", staffId)
                .update();
    }

    /**
     * Two statements, old owner first: the one-owner unique index rejects two owners, even within one statement.
     */
    void transferOwnership(UUID currentOwnerId, UUID nextOwnerId) {
        setOwner(currentOwnerId, false);
        setOwner(nextOwnerId, true);
    }

    /**
     * Staff may only be added to a syndicate that exists. Asked here, not of management, so staff does not depend on it.
     */
    boolean syndicateExists(UUID syndicateId) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM syndicates WHERE id = :id)")
                .param("id", syndicateId)
                .query(Boolean.class)
                .single();
    }

    private UUID insert(UUID syndicateId, UUID userId, String title, boolean owner) {
        return jdbc.sql("""
                        INSERT INTO syndicate_staff (syndicate_id, user_id, title, is_owner)
                        VALUES (:syndicateId, :userId, :title, :owner)
                        RETURNING id
                        """)
                .param("syndicateId", syndicateId)
                .param("userId", userId)
                .param("title", title)
                .param("owner", owner)
                .query(UUID.class)
                .single();
    }

    private void setOwner(UUID staffId, boolean owner) {
        jdbc.sql("UPDATE syndicate_staff SET is_owner = :owner WHERE id = :id")
                .param("owner", owner)
                .param("id", staffId)
                .update();
    }
}