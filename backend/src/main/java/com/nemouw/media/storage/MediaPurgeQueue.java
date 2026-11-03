package com.nemouw.media.storage;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
class MediaPurgeQueue {
    record Entry(long id, String s3Key) {
    }

    private final JdbcClient jdbc;

    MediaPurgeQueue(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Locks the rows until the transaction ends; other instances skip them. */
    List<Entry> claim(int limit) {
        return jdbc.sql("""
                        SELECT id, s3_key FROM media_purge_queue
                        ORDER BY id LIMIT :limit
                        FOR UPDATE SKIP LOCKED""")
                .param("limit", limit)
                .query(Entry.class)
                .list();
    }

    void remove(Collection<Long> ids) {
        if (ids.isEmpty()) return;
        jdbc.sql("DELETE FROM media_purge_queue WHERE id IN (:ids)")
                .param("ids", ids)
                .update();
    }
}