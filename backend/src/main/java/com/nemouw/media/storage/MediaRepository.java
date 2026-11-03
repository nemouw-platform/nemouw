package com.nemouw.media.storage;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public class MediaRepository {
    private final JdbcClient jdbc;

    MediaRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public MediaRow insert(String s3Key, String mimeType) {
        return jdbc.sql("""
                        INSERT INTO media (s3_key, mime_type) VALUES (:key, :mime)
                        RETURNING id, s3_key, mime_type, created_at""")
                .param("key", s3Key)
                .param("mime", mimeType)
                .query(MediaRow.class)
                .single();
    }

    public Optional<String> findKey(UUID id) {
        return jdbc.sql("SELECT s3_key FROM media WHERE id = :id")
                .param("id", id)
                .query(String.class)
                .optional();
    }

    Set<String> existingKeys(Collection<String> keys) {
        if (keys.isEmpty()) return Set.of();
        return new HashSet<>(jdbc.sql("SELECT s3_key FROM media WHERE s3_key IN (:keys)")
                .param("keys", keys)
                .query(String.class)
                .list());
    }
}