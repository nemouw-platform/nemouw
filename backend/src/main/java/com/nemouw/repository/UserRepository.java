package com.nemouw.repository;

import com.nemouw.enums.UserIdentifierType;
import com.nemouw.enums.UserRole;
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

    public record EmailIdentity(UUID identifierId, UUID userId, String status, String roleCode) {}

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

    public void activate(Handle h, UUID userId, UUID identifierId) {
        h.createUpdate("UPDATE users SET status = 'active' WHERE id = :id AND status = 'pending'")
                .bind("id", userId).execute();
        h.createUpdate("UPDATE user_identifiers SET verified_at = now() WHERE id = :id")
                .bind("id", identifierId).execute();
    }
}
