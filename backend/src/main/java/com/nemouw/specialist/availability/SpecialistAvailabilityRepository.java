package com.nemouw.specialist.availability;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.core.simple.JdbcClient.StatementSpec;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class SpecialistAvailabilityRepository {
    private static final String COLUMNS = "id, location_id, day_of_week, start_time, end_time";
    private static final String RETURNING = " RETURNING " + COLUMNS;

    private final JdbcClient jdbc;

    SpecialistAvailabilityRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    List<SpecialistAvailabilityRow> findAll(UUID specialistId) {
        return jdbc.sql("SELECT " + COLUMNS + """
                         FROM specialist_location_availabilities
                        WHERE specialist_id = :specialistId
                        ORDER BY day_of_week, start_time, id
                        """)
                .param("specialistId", specialistId)
                .query(SpecialistAvailabilityRow.class)
                .list();
    }

    SpecialistAvailabilityRow insert(UUID specialistId, AvailabilityFields f) {
        return bind(jdbc.sql("""
                        INSERT INTO specialist_location_availabilities
                            (specialist_id, location_id, day_of_week, start_time, end_time)
                        VALUES (:specialistId, :locationId, :dayOfWeek, :startTime, :endTime)
                        """ + RETURNING), f)
                .param("specialistId", specialistId)
                .query(SpecialistAvailabilityRow.class)
                .single();
    }

    Optional<SpecialistAvailabilityRow> update(UUID specialistId, UUID id, AvailabilityFields f) {
        return bind(jdbc.sql("""
                        UPDATE specialist_location_availabilities
                        SET location_id = :locationId, day_of_week = :dayOfWeek,
                            start_time = :startTime, end_time = :endTime
                        WHERE id = :id AND specialist_id = :specialistId
                        """ + RETURNING), f)
                .param("id", id)
                .param("specialistId", specialistId)
                .query(SpecialistAvailabilityRow.class)
                .optional();
    }

    boolean delete(UUID specialistId, UUID id) {
        return jdbc.sql("DELETE FROM specialist_location_availabilities WHERE id = :id AND specialist_id = :specialistId")
                .param("id", id)
                .param("specialistId", specialistId)
                .update() > 0;
    }

    boolean locationBelongsTo(UUID specialistId, UUID locationId) {
        return jdbc.sql("""
                        SELECT EXISTS (SELECT 1 FROM specialist_locations
                                       WHERE id = :locationId AND specialist_id = :specialistId)
                        """)
                .param("locationId", locationId)
                .param("specialistId", specialistId)
                .query(Boolean.class)
                .single();
    }

    private static StatementSpec bind(StatementSpec spec, AvailabilityFields f) {
        return spec.param("locationId", f.locationId())
                .param("dayOfWeek", f.dayOfWeek())
                .param("startTime", f.startTime())
                .param("endTime", f.endTime());
    }
}