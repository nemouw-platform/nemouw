package com.nemouw.syndicate.management;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class SyndicateRepository {
    private static final String COLUMNS = "id, name, specialty_id, country_id, contact_email, contact_phone, "
            + "status AS status_code, created_at";
    private static final String SELECT = "SELECT " + COLUMNS + " FROM syndicates ";
    private static final String RETURNING = "RETURNING " + COLUMNS;

    private final JdbcClient jdbc;

    SyndicateRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Null filters are ignored. */
    List<Syndicate> findAll(UUID specialtyId, UUID countryId) {
        return jdbc.sql(SELECT + """
                        WHERE (CAST(:specialtyId AS uuid) IS NULL OR specialty_id = :specialtyId)
                          AND (CAST(:countryId AS uuid) IS NULL OR country_id = :countryId)
                        ORDER BY name
                        """)
                .param("specialtyId", specialtyId)
                .param("countryId", countryId)
                .query(Syndicate.class)
                .list();
    }

    Optional<Syndicate> findById(UUID id) {
        return jdbc.sql(SELECT + "WHERE id = :id")
                .param("id", id)
                .query(Syndicate.class)
                .optional();
    }

    boolean existsById(UUID id) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM syndicates WHERE id = :id)")
                .param("id", id)
                .query(Boolean.class)
                .single();
    }

    boolean existsByCountryAndSpecialty(UUID countryId, UUID specialtyId) {
        return jdbc.sql("""
                        SELECT EXISTS (SELECT 1 FROM syndicates
                                       WHERE country_id = :countryId AND specialty_id = :specialtyId)
                        """)
                .param("countryId", countryId)
                .param("specialtyId", specialtyId)
                .query(Boolean.class)
                .single();
    }

    /** Throws DuplicateKeyException when the country already has a syndicate for this specialty. */
    Syndicate insert(String name, UUID specialtyId, UUID countryId, String contactEmail, String contactPhone) {
        return jdbc.sql("""
                        INSERT INTO syndicates (name, specialty_id, country_id, contact_email, contact_phone)
                        VALUES (:name, :specialtyId, :countryId, :email, :phone)
                        """ + RETURNING)
                .param("name", name)
                .param("specialtyId", specialtyId)
                .param("countryId", countryId)
                .param("email", contactEmail)
                .param("phone", contactPhone)
                .query(Syndicate.class)
                .single();
    }

    Syndicate update(UUID id, String name, String contactEmail, String contactPhone) {
        return jdbc.sql("""
                        UPDATE syndicates SET name = :name, contact_email = :email, contact_phone = :phone
                        WHERE id = :id
                        """ + RETURNING)
                .param("id", id)
                .param("name", name)
                .param("email", contactEmail)
                .param("phone", contactPhone)
                .query(Syndicate.class)
                .single();
    }
}