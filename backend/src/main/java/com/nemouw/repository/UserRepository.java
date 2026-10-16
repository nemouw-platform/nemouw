package com.nemouw.repository;

import com.nemouw.enums.UserRole;
import com.nemouw.enums.UserStatus;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;

import java.util.Optional;
import java.util.UUID;

public class UserRepository {

    public UUID insertUser(Handle h, UserRole role, String firstName, String lastName) {
        return h.createQuery("""
                        INSERT INTO users (role_id, first_name, last_name)
                        SELECT id, :first, :last FROM user_roles WHERE code = :role
                        RETURNING id""")
                .bind("first", firstName).bind("last", lastName).bind("role", role.code())
                .mapTo(UUID.class).one();
    }

    public UUID insertEmailIdentifier(Handle h, UUID userId, String email) {
        return h.createQuery("""
                        INSERT INTO user_identifiers (user_id, identifier_type, identifier, is_primary)
                        VALUES (:u, 'email', :e, true) RETURNING id""")
                .bind("u", userId).bind("e", email)
                .mapTo(UUID.class).one();
    }

    public void insertPassword(Handle h, UUID userId, String hash) {
        h.createUpdate("INSERT INTO user_passwords (user_id, password_hash) VALUES (:u, :h)")
                .bind("u", userId).bind("h", hash).execute();
    }

    public void replacePassword(Handle h, UUID userId, String hash) {
        h.createUpdate("UPDATE user_passwords SET password_hash = :hash WHERE user_id = :userId")
                .bind("userId", userId)
                .bind("hash", hash)
                .execute();
    }

    public void updateName(Handle h, UUID userId, String firstName, String lastName) {
        h.createUpdate("UPDATE users SET first_name = :first, last_name = :last WHERE id = :id")
                .bind("first", firstName).bind("last", lastName).bind("id", userId).execute();
    }

    public record EmailIdentity(UUID identifierId, UUID userId, String status, String roleCode) {
        public UserStatus userStatus() {
            return UserStatus.fromCode(status);
        }
    }

    public Optional<EmailIdentity> findByEmail(Handle h, String email) {
        return h.createQuery("""
                        SELECT ui.id AS identifier_id, ui.user_id, u.status, r.code AS role_code
                        FROM user_identifiers ui
                        JOIN users u ON u.id = ui.user_id
                        JOIN user_roles r ON r.id = u.role_id
                        WHERE ui.identifier_type = 'email' AND ui.identifier = :e""")
                .bind("e", email)
                .map(ConstructorMapper.of(EmailIdentity.class)).findFirst();
    }

    /**
     * Locks the user row until the transaction ends and returns its current status.
     */
    public Optional<UserStatus> lockStatus(Handle h, UUID userId) {
        return h.createQuery("SELECT status FROM users WHERE id = :id FOR UPDATE")
                .bind("id", userId)
                .mapTo(String.class).findOne()
                .map(UserStatus::fromCode);
    }

    /**
     * Activates only a still-pending user. Returns false (and changes nothing) otherwise.
     */
    public boolean activate(Handle h, UUID userId, UUID identifierId) {
        int updated = h.createUpdate("UPDATE users SET status = 'active' WHERE id = :id AND status = 'pending'")
                .bind("id", userId).execute();
        if (updated == 0) return false;
        h.createUpdate("UPDATE user_identifiers SET verified_at = now() WHERE id = :id")
                .bind("id", identifierId).execute();
        return true;
    }

    public record LoginRow(UUID id, String status, String roleCode, String passwordHash) {
        public UserStatus userStatus() {
            return UserStatus.fromCode(status);
        }
    }

    /**
     * identifier can be an email, phone or username. The caller passes it trimmed and lowercased.
     */
    public Optional<LoginRow> findLoginByIdentifier(Handle h, String identifier) {
        return h.createQuery("""
                        SELECT u.id, u.status, r.code AS role_code, p.password_hash
                        FROM user_identifiers ui
                        JOIN users u ON u.id = ui.user_id
                        JOIN user_roles r ON r.id = u.role_id
                        LEFT JOIN user_passwords p ON p.user_id = u.id
                        WHERE ui.identifier = :i""")
                .bind("i", identifier)
                .map(ConstructorMapper.of(LoginRow.class)).findFirst();
    }

    public void touchLastLogin(Handle h, UUID userId) {
        h.createUpdate("UPDATE users SET last_login_at = now() WHERE id = :id").bind("id", userId).execute();
    }
}