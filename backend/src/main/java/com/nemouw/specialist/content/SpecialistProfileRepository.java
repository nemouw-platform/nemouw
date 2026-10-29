package com.nemouw.specialist.content;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class SpecialistProfileRepository {
    private static final String COLUMNS = "specialist_id, locale_code, title, headline, bio, created_at, updated_at";

    private final JdbcClient jdbc;

    SpecialistProfileRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    List<SpecialistProfileRow> findAll(UUID specialistId) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM specialist_profile WHERE specialist_id = :id ORDER BY locale_code")
                .param("id", specialistId)
                .query(SpecialistProfileRow.class)
                .list();
    }

    Optional<SpecialistProfileRow> find(UUID specialistId, String locale) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM specialist_profile WHERE specialist_id = :id AND locale_code = :locale")
                .param("id", specialistId)
                .param("locale", locale)
                .query(SpecialistProfileRow.class)
                .optional();
    }

    /** One statement, so two concurrent PUTs for the same locale cannot collide. updated_at is set by trigger. */
    SpecialistProfileRow upsert(UUID specialistId, String locale, String title, String headline, String bio) {
        return jdbc.sql("""
                    INSERT INTO specialist_profile (specialist_id, locale_code, title, headline, bio)
                    VALUES (:id, :locale, :title, :headline, :bio)
                    ON CONFLICT (specialist_id, locale_code)
                    DO UPDATE SET title = EXCLUDED.title, headline = EXCLUDED.headline, bio = EXCLUDED.bio
                    RETURNING""" + " " + COLUMNS)
                .param("id", specialistId)
                .param("locale", locale)
                .param("title", title)
                .param("headline", headline)
                .param("bio", bio)
                .query(SpecialistProfileRow.class)
                .single();
    }

    boolean delete(UUID specialistId, String locale) {
        return jdbc.sql("DELETE FROM specialist_profile WHERE specialist_id = :id AND locale_code = :locale")
                .param("id", specialistId)
                .param("locale", locale)
                .update() > 0;
    }
}