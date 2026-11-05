package com.nemouw.specialist.credential;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.core.simple.JdbcClient.StatementSpec;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class CredentialRepository {
    private static final String SELECT = """
            SELECT c.id, c.specialist_id, c.credential_type AS credential_type_code, c.title,
                   c.issuing_organization, c.credential_number, c.issue_date, c.expiry_date, c.document_id,
                   m.mime_type AS document_mime_type, m.created_at AS document_uploaded_at,
                   c.verification_status AS verification_status_code, c.reviewed_by, c.reviewed_at,
                   c.rejection_reason, c.created_at, c.updated_at
            FROM specialist_credentials c
            LEFT JOIN media m ON m.id = c.document_id
            """;
    private static final String BY_OWNER = "WHERE c.id = :id AND c.specialist_id = :specialistId";
    private static final String BY_ID = "WHERE c.id = :id";
    private static final String LOCK = " FOR UPDATE OF c";

    /**
     * An owner change puts the credential back in the review queue.
     */
    private static final String RESET_REVIEW =
            "verification_status = :pending, reviewed_by = NULL, reviewed_at = NULL, rejection_reason = NULL";

    private final JdbcClient jdbc;

    CredentialRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    List<CredentialRow> findBySpecialist(UUID specialistId) {
        return jdbc.sql(SELECT + "WHERE c.specialist_id = :specialistId ORDER BY c.created_at DESC, c.id")
                .param("specialistId", specialistId)
                .query(CredentialRow.class)
                .list();
    }

    /**
     * A null status returns every status. Oldest first, so the queue is worked in order.
     */
    List<CredentialRow> findBySyndicate(UUID syndicateId, CredentialVerificationStatus status) {
        return jdbc.sql(SELECT + """
                        JOIN specialists s ON s.id = c.specialist_id
                        WHERE s.syndicate_id = :syndicateId
                          AND (CAST(:status AS text) IS NULL OR c.verification_status = :status)
                        ORDER BY c.created_at, c.id
                        """)
                .param("syndicateId", syndicateId)
                .param("status", status == null ? null : status.code())
                .query(CredentialRow.class)
                .list();
    }

    Optional<CredentialRow> findOwned(UUID id, UUID specialistId) {
        return owned(SELECT + BY_OWNER, id, specialistId);
    }

    Optional<CredentialRow> lockOwned(UUID id, UUID specialistId) {
        return owned(SELECT + BY_OWNER + LOCK, id, specialistId);
    }

    Optional<CredentialRow> find(UUID id) {
        return jdbc.sql(SELECT + BY_ID).param("id", id).query(CredentialRow.class).optional();
    }

    Optional<CredentialRow> lock(UUID id) {
        return jdbc.sql(SELECT + BY_ID + LOCK).param("id", id).query(CredentialRow.class).optional();
    }

    UUID insert(UUID specialistId, CredentialFields f, UUID documentId) {
        return bind(jdbc.sql("""
                INSERT INTO specialist_credentials
                    (specialist_id, credential_type, title, issuing_organization, credential_number,
                     issue_date, expiry_date, document_id)
                VALUES (:specialistId, :type, :title, :organization, :number,
                        :issueDate, :expiryDate, :documentId)
                RETURNING id"""), f)
                .param("specialistId", specialistId)
                .param("documentId", documentId)
                .query(UUID.class)
                .single();
    }

    void update(UUID id, CredentialFields f) {
        bind(jdbc.sql("""
            UPDATE specialist_credentials
            SET credential_type = :type, title = :title, issuing_organization = :organization,
                credential_number = :number, issue_date = :issueDate, expiry_date = :expiryDate,
            """ + RESET_REVIEW + " WHERE id = :id"), f)
                .param("pending", CredentialVerificationStatus.PENDING.code())
                .param("id", id)
                .update();
    }

    /** The release trigger deletes the replaced media row and queues its object for purge. */
    void replaceDocument(UUID id, UUID mediaId) {
        jdbc.sql("UPDATE specialist_credentials SET document_id = :mediaId, " + RESET_REVIEW + " WHERE id = :id")
                .param("mediaId", mediaId)
                .param("pending", CredentialVerificationStatus.PENDING.code())
                .param("id", id)
                .update();
    }

    void delete(UUID id) {
        jdbc.sql("DELETE FROM specialist_credentials WHERE id = :id").param("id", id).update();
    }

    void markVerified(UUID id, UUID reviewerId) {
        jdbc.sql("""
                    UPDATE specialist_credentials
                    SET verification_status = :status, reviewed_by = :reviewer, reviewed_at = now(),
                        rejection_reason = NULL
                    WHERE id = :id""")
                .param("status", CredentialVerificationStatus.VERIFIED.code())
                .param("reviewer", reviewerId)
                .param("id", id)
                .update();
    }

    void markRejected(UUID id, UUID reviewerId, String reason) {
        jdbc.sql("""
                    UPDATE specialist_credentials
                    SET verification_status = :status, reviewed_by = :reviewer, reviewed_at = now(),
                        rejection_reason = :reason
                    WHERE id = :id""")
                .param("status", CredentialVerificationStatus.REJECTED.code())
                .param("reviewer", reviewerId)
                .param("reason", reason)
                .param("id", id)
                .update();
    }

    boolean isStaffOfCredentialSyndicate(UUID credentialId, UUID userId) {
        return jdbc.sql("""
                        SELECT EXISTS (
                            SELECT 1 FROM specialist_credentials c
                            JOIN specialists s ON s.id = c.specialist_id
                            JOIN syndicate_staff ss ON ss.syndicate_id = s.syndicate_id
                            WHERE c.id = :credentialId AND ss.user_id = :userId)""")
                .param("credentialId", credentialId)
                .param("userId", userId)
                .query(Boolean.class)
                .single();
    }

    private Optional<CredentialRow> owned(String sql, UUID id, UUID specialistId) {
        return jdbc.sql(sql)
                .param("id", id)
                .param("specialistId", specialistId)
                .query(CredentialRow.class)
                .optional();
    }

    private static StatementSpec bind(StatementSpec spec, CredentialFields f) {
        return spec.param("type", f.type().code())
                .param("title", f.title())
                .param("organization", f.issuingOrganization())
                .param("number", f.credentialNumber())
                .param("issueDate", f.issueDate())
                .param("expiryDate", f.expiryDate());
    }
}