package com.nemouw.specialist.image;

import com.nemouw.media.api.MediaKind;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
class SpecialistImageRepository {
    private final JdbcClient jdbc;

    SpecialistImageRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** The release trigger deletes the replaced media row and queues its object for purge. */
    void link(UUID specialistId, MediaKind kind, UUID mediaId) {
        assign(specialistId, kind, mediaId);
    }

    void unlink(UUID specialistId, MediaKind kind) {
        assign(specialistId, kind, null);
    }

    private void assign(UUID specialistId, MediaKind kind, UUID mediaId) {
        jdbc.sql("UPDATE specialists SET " + column(kind) + " = :mediaId WHERE id = :id")
                .param("mediaId", mediaId)
                .param("id", specialistId)
                .update();
    }

    private static String column(MediaKind kind) {
        return switch (kind) {
            case PROFILE_PICTURE -> "profile_picture_id";
            case COVER_IMAGE -> "cover_image_id";
            case CREDENTIAL_DOCUMENT -> throw new IllegalArgumentException("Not a specialist image: " + kind);
        };
    }
}