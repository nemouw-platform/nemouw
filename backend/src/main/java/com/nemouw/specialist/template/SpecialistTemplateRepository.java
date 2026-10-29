package com.nemouw.specialist.template;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
class SpecialistTemplateRepository {
    private static final String COLUMNS = "specialist_id, primary_color, created_at, updated_at";

    private final JdbcClient jdbc;

    SpecialistTemplateRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    Optional<SpecialistTemplateRow> find(UUID specialistId) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM specialist_template WHERE specialist_id = :specialistId")
                .param("specialistId", specialistId)
                .query(SpecialistTemplateRow.class)
                .optional();
    }

    /** Creates the row if missing. The color is overwritten only when {@code colorSent}; otherwise it is kept. */
    SpecialistTemplateRow upsert(UUID specialistId, boolean colorSent, String primaryColor) {
        return jdbc.sql("""
                        INSERT INTO specialist_template (specialist_id, primary_color)
                        VALUES (:specialistId, :primaryColor)
                        ON CONFLICT (specialist_id) DO UPDATE
                        SET primary_color = CASE WHEN :colorSent THEN EXCLUDED.primary_color
                                                 ELSE specialist_template.primary_color END
                        RETURNING \
                        """ + COLUMNS)
                .param("specialistId", specialistId)
                .param("primaryColor", primaryColor)
                .param("colorSent", colorSent)
                .query(SpecialistTemplateRow.class)
                .single();
    }
}