package com.nemouw.syndicate.access;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * The two membership questions authorization asks. Owns its queries so that access depends on nothing else in
 * syndicate: management and staff use the access annotations.
 */
@Repository
class SyndicateMembershipRepository {
    private final JdbcClient jdbc;

    SyndicateMembershipRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    boolean isStaff(UUID syndicateId, UUID userId) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM syndicate_staff WHERE syndicate_id = :sid AND user_id = :uid)")
                .param("sid", syndicateId)
                .param("uid", userId)
                .query(Boolean.class)
                .single();
    }

    boolean isOwner(UUID syndicateId, UUID userId) {
        return jdbc.sql("""
                        SELECT EXISTS (SELECT 1 FROM syndicate_staff
                                       WHERE syndicate_id = :sid AND user_id = :uid AND is_owner)
                        """)
                .param("sid", syndicateId)
                .param("uid", userId)
                .query(Boolean.class)
                .single();
    }
}