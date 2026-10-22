package com.nemouw.auth.repository;

import com.nemouw.enums.UserRole;
import com.nemouw.enums.UserStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public class UserRepository {
    private final JdbcClient jdbc;

    public UserRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public UUID insertUser(UserRole role, String firstName, String lastName) {
        return jdbc.sql("""
                        INSERT INTO users (role_id, first_name, last_name)
                        SELECT id, :first, :last FROM user_roles WHERE code = :role
                        RETURNING id""")
                .param("first", firstName).param("last", lastName).param("role", role.code())
                .query(UUID.class).single();
    }

    public UUID insertEmailIdentifier(UUID userId, String email) {
        return jdbc.sql("""
                        INSERT INTO user_identifiers (user_id, identifier_type, identifier, is_primary)
                        VALUES (:u, 'email', :e, true) RETURNING id""")
                .param("u", userId).param("e", email)
                .query(UUID.class).single();
    }

    public void replacePassword(UUID userId, String hash) {
        jdbc.sql("""
                        INSERT INTO user_passwords (user_id, password_hash) VALUES (:userId, :hash)
                        ON CONFLICT (user_id) DO UPDATE SET password_hash = EXCLUDED.password_hash""")
                .param("userId", userId).param("hash", hash)
                .update();
    }

    public void updateName(UUID userId, String firstName, String lastName) {
        jdbc.sql("UPDATE users SET first_name = :first, last_name = :last WHERE id = :id")
                .param("first", firstName).param("last", lastName).param("id", userId)
                .update();
    }

    public record EmailIdentity(UUID identifierId, UUID userId, String email, String status, String roleCode) {
        public UserStatus userStatus() {
            return UserStatus.fromCode(status);
        }
    }

    private static final String EMAIL_IDENTITY = """
            SELECT ui.id AS identifier_id, ui.user_id, ui.identifier AS email, u.status, r.code AS role_code
            FROM user_identifiers ui
            JOIN users u ON u.id = ui.user_id
            JOIN user_roles r ON r.id = u.role_id
            WHERE ui.identifier_type = 'email'""";

    public Optional<EmailIdentity> findByEmail(String email) {
        return jdbc.sql(EMAIL_IDENTITY + " AND ui.identifier = :e")
                .param("e", email)
                .query(EmailIdentity.class).list().stream().findFirst();
    }

    /** The user's primary email (the one signup created). Used when only the user id is known, e.g. from a token. */
    public Optional<EmailIdentity> findPrimaryEmail(UUID userId) {
        return jdbc.sql(EMAIL_IDENTITY + " AND ui.user_id = :id AND ui.is_primary")
                .param("id", userId)
                .query(EmailIdentity.class).list().stream().findFirst();
    }

    /**
     * Locks the user row until the transaction ends and returns its current status.
     * Only meaningful inside a transaction, so it refuses to run outside one.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<UserStatus> lockStatus(UUID userId) {
        return jdbc.sql("SELECT status FROM users WHERE id = :id FOR UPDATE")
                .param("id", userId)
                .query(String.class).optional()
                .map(UserStatus::fromCode);
    }

    /**
     * Activates only a still-pending user. Returns false (and changes nothing) otherwise.
     * Used by signup once the password is set, when the email was already marked verified.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean activate(UUID userId) {
        return jdbc.sql("UPDATE users SET status = 'active' WHERE id = :id AND status = 'pending'")
                .param("id", userId)
                .update() > 0;
    }

    /**
     * Activates a pending user AND marks the email verified (password reset: the link proves mailbox control).
     * Two statements that must commit together, so it must run inside the caller's transaction.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean activate(UUID userId, UUID identifierId) {
        if (!activate(userId)) return false;
        markVerified(identifierId);
        return true;
    }

    /** Records when the mailbox was proven. Keeps the first time if it was already verified. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void markVerified(UUID identifierId) {
        jdbc.sql("UPDATE user_identifiers SET verified_at = coalesce(verified_at, now()) WHERE id = :id")
                .param("id", identifierId)
                .update();
    }

    public record LoginRow(UUID id, String status, String roleCode, String passwordHash) {
        public UserStatus userStatus() {
            return UserStatus.fromCode(status);
        }
    }

    /**
     * identifier can be an email, phone or username. The caller passes it trimmed and lowercased.
     */
    public Optional<LoginRow> findLoginByIdentifier(String identifier) {
        return jdbc.sql("""
                        SELECT u.id, u.status, r.code AS role_code, p.password_hash
                        FROM user_identifiers ui
                        JOIN users u ON u.id = ui.user_id
                        JOIN user_roles r ON r.id = u.role_id
                        LEFT JOIN user_passwords p ON p.user_id = u.id
                        WHERE ui.identifier = :i""")
                .param("i", identifier)
                .query(LoginRow.class).list().stream().findFirst();
    }

    public void touchLastLogin(UUID userId) {
        jdbc.sql("UPDATE users SET last_login_at = now() WHERE id = :id")
                .param("id", userId)
                .update();
    }
}