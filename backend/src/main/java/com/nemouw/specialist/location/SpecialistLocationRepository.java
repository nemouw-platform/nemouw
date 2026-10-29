package com.nemouw.specialist.location;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.core.simple.JdbcClient.StatementSpec;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class SpecialistLocationRepository {
    private static final String COLUMNS = "id, country_id, label, address_line1, address_line2, city, "
            + "latitude, longitude, phone, is_primary";
    private static final String RETURNING = " RETURNING " + COLUMNS;

    private final JdbcClient jdbc;

    SpecialistLocationRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    List<SpecialistLocationRow> findAll(UUID specialistId) {
        return jdbc.sql("SELECT " + COLUMNS + """
                         FROM specialist_locations
                        WHERE specialist_id = :specialistId
                        ORDER BY is_primary DESC, created_at, id
                        """)
                .param("specialistId", specialistId)
                .query(SpecialistLocationRow.class)
                .list();
    }

    SpecialistLocationRow insert(UUID specialistId, LocationFields f) {
        return bind(jdbc.sql("""
                        INSERT INTO specialist_locations
                            (specialist_id, country_id, label, address_line1, address_line2, city,
                             latitude, longitude, phone, is_primary)
                        VALUES (:specialistId, :countryId, :label, :addressLine1, :addressLine2, :city,
                                :latitude, :longitude, :phone, :primary)
                        """ + RETURNING), f)
                .param("specialistId", specialistId)
                .query(SpecialistLocationRow.class)
                .single();
    }

    Optional<SpecialistLocationRow> update(UUID specialistId, UUID id, LocationFields f) {
        return bind(jdbc.sql("""
                        UPDATE specialist_locations
                        SET country_id = :countryId, label = :label, address_line1 = :addressLine1,
                            address_line2 = :addressLine2, city = :city, latitude = :latitude,
                            longitude = :longitude, phone = :phone, is_primary = :primary
                        WHERE id = :id AND specialist_id = :specialistId
                        """ + RETURNING), f)
                .param("id", id)
                .param("specialistId", specialistId)
                .query(SpecialistLocationRow.class)
                .optional();
    }

    boolean delete(UUID specialistId, UUID id) {
        return jdbc.sql("DELETE FROM specialist_locations WHERE id = :id AND specialist_id = :specialistId")
                .param("id", id)
                .param("specialistId", specialistId)
                .update() > 0;
    }

    void clearPrimary(UUID specialistId) {
        jdbc.sql("UPDATE specialist_locations SET is_primary = false WHERE specialist_id = :specialistId AND is_primary")
                .param("specialistId", specialistId)
                .update();
    }

    /** Serializes primary changes for one specialist, so two requests cannot both claim it. */
    void lockOwner(UUID specialistId) {
        jdbc.sql("SELECT id FROM specialists WHERE id = :id FOR UPDATE")
                .param("id", specialistId)
                .query(UUID.class)
                .optional();
    }

    boolean countryExists(UUID countryId) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM countries WHERE id = :id)")
                .param("id", countryId)
                .query(Boolean.class)
                .single();
    }

    private static StatementSpec bind(StatementSpec spec, LocationFields f) {
        return spec.param("countryId", f.countryId())
                .param("label", f.label())
                .param("addressLine1", f.addressLine1())
                .param("addressLine2", f.addressLine2())
                .param("city", f.city())
                .param("latitude", f.latitude())
                .param("longitude", f.longitude())
                .param("phone", f.phone())
                .param("primary", f.primary());
    }
}