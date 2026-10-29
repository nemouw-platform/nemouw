package com.nemouw.specialist.profile;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;

@Repository
class SpecialistRepository {
    private static final String SELECT = """
            SELECT s.id, s.user_id, s.specialty_id, s.syndicate_id, s.syndicate_member_number, s.username,
                   s.gender, s.whatsapp_number, s.primary_language, s.years_of_experience,
                   s.profile_picture_id, pm.s3_key AS profile_picture_key,
                   s.cover_image_id, cm.s3_key AS cover_image_key,
                   s.min_age_treated, s.max_age_treated,
                   s.verification_status AS verification_status_code, s.verified_at, s.created_at, s.updated_at
            FROM specialists s
            LEFT JOIN media pm ON pm.id = s.profile_picture_id
            LEFT JOIN media cm ON cm.id = s.cover_image_id
            """;

    private static final Set<String> UPDATABLE = Set.of("username", "syndicate_id", "syndicate_member_number",
            "gender", "whatsapp_number", "primary_language", "years_of_experience", "profile_picture_id",
            "cover_image_id", "min_age_treated", "max_age_treated");

    private final JdbcClient jdbc;

    SpecialistRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    Optional<SpecialistRow> findByUserId(UUID userId) {
        return jdbc.sql(SELECT + "WHERE s.user_id = :userId")
                .param("userId", userId)
                .query(SpecialistRow.class)
                .optional();
    }

    Optional<UUID> findIdByUserId(UUID userId) {
        return jdbc.sql("SELECT id FROM specialists WHERE user_id = :userId")
                .param("userId", userId)
                .query(UUID.class)
                .optional();
    }

    Optional<SpecialistRow> findById(UUID id) {
        return jdbc.sql(SELECT + "WHERE s.id = :id")
                .param("id", id)
                .query(SpecialistRow.class)
                .optional();
    }

    boolean syndicateExists(UUID syndicateId) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM syndicates WHERE id = :id)")
                .param("id", syndicateId)
                .query(Boolean.class)
                .single();
    }

    /** excludeId may be null (onboarding) or the caller's own id (PATCH). */
    boolean usernameTaken(String username, UUID excludeId) {
        return jdbc.sql("""
                        SELECT EXISTS (SELECT 1 FROM specialists
                                       WHERE username = :username
                                         AND (CAST(:excludeId AS uuid) IS NULL OR id <> :excludeId))
                        """)
                .param("username", username)
                .param("excludeId", excludeId)
                .query(Boolean.class)
                .single();
    }

    boolean memberNumberTaken(UUID syndicateId, String memberNumber, UUID excludeId) {
        return jdbc.sql("""
                        SELECT EXISTS (SELECT 1 FROM specialists
                                       WHERE syndicate_id = :syndicateId AND syndicate_member_number = :number
                                         AND (CAST(:excludeId AS uuid) IS NULL OR id <> :excludeId))
                        """)
                .param("syndicateId", syndicateId)
                .param("number", memberNumber)
                .param("excludeId", excludeId)
                .query(Boolean.class)
                .single();
    }

    /** The media exists and is not a credential document or another specialist's picture or cover. */
    boolean mediaAvailable(UUID mediaId, UUID specialistId) {
        return jdbc.sql("""
                        SELECT EXISTS (
                            SELECT 1 FROM media m
                            WHERE m.id = :mediaId
                              AND NOT EXISTS (SELECT 1 FROM specialist_credentials c WHERE c.document_id = m.id)
                              AND NOT EXISTS (SELECT 1 FROM specialists s
                                              WHERE s.id <> :specialistId
                                                AND (s.profile_picture_id = m.id OR s.cover_image_id = m.id)))
                        """)
                .param("mediaId", mediaId)
                .param("specialistId", specialistId)
                .query(Boolean.class)
                .single();
    }

    /**
     * specialty_id is not inserted: the trg_specialists_derive_specialty trigger fills it from the syndicate.
     * verification_status takes its 'pending' default.
     */
    UUID insert(UUID userId, String username, UUID syndicateId, String memberNumber) {
        return jdbc.sql("""
                        INSERT INTO specialists (user_id, syndicate_id, syndicate_member_number, username)
                        VALUES (:userId, :syndicateId, :number, :username)
                        RETURNING id
                        """)
                .param("userId", userId)
                .param("syndicateId", syndicateId)
                .param("number", memberNumber)
                .param("username", username)
                .query(UUID.class)
                .single();
    }

    void update(UUID id, Map<String, Object> changes) {
        if (changes.isEmpty()) return;
        StringJoiner set = new StringJoiner(", ");
        for (String column : changes.keySet()) {
            if (!UPDATABLE.contains(column)) throw new IllegalArgumentException("Not updatable: " + column);
            set.add(column + " = :" + column);
        }
        jdbc.sql("UPDATE specialists SET " + set + " WHERE id = :id")
                .params(changes)
                .param("id", id)
                .update();
    }
}