package com.nemouw.media.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/** Deletes objects no media row points to: leftovers of a failed rollback cleanup or a crash after upload. */
@Component
class MediaReconcileJob {
    private static final Logger log = LoggerFactory.getLogger(MediaReconcileJob.class);
    private static final Duration GRACE = Duration.ofHours(24);

    private final ObjectStorage storage;
    private final MediaRepository media;

    MediaReconcileJob(ObjectStorage storage, MediaRepository media) {
        this.storage = storage;
        this.media = media;
    }

    @Scheduled(initialDelayString = "PT5M", fixedDelayString = "PT24H")
    void deleteUnreferencedObjects() {
        storage.scanOlderThan(Instant.now().minus(GRACE), keys -> {
            Set<String> known = media.existingKeys(keys);
            List<String> orphans = keys.stream().filter(k -> !known.contains(k)).toList();
            Set<String> deleted = storage.delete(orphans);
            if (!deleted.isEmpty()) log.warn("media_orphans_deleted count={}", deleted.size());
        });
    }
}