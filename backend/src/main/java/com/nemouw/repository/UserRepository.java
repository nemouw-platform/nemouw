package com.nemouw.repository;

import com.nemouw.enums.UserIdentifierType;
import com.nemouw.enums.UserRole;
import org.jdbi.v3.core.Handle;

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
}
