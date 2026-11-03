package com.nemouw.media.storage;

import com.nemouw.media.storage.MediaPurgeQueue.Entry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Set;

/** Deletes from S3 the objects whose media rows were deleted. The queue is filled by database triggers. */
@Component
class MediaPurgeJob {
    private static final int BATCH = 100;

    private final MediaPurgeQueue queue;
    private final ObjectStorage storage;
    private final TransactionTemplate tx;

    MediaPurgeJob(MediaPurgeQueue queue, ObjectStorage storage, TransactionTemplate tx) {
        this.queue = queue;
        this.storage = storage;
        this.tx = tx;
    }

    @Scheduled(initialDelayString = "PT30S", fixedDelayString = "PT1M")
    void drainQueue() {
        while (Boolean.TRUE.equals(tx.execute(status -> drainBatch()))) {
            // batches keep coming back full
        }
    }

    /** A storage failure rolls back, so the rows stay queued for the next run. */
    private boolean drainBatch() {
        List<Entry> batch = queue.claim(BATCH);
        Set<String> deleted = storage.delete(batch.stream().map(Entry::s3Key).toList());
        List<Long> done = batch.stream().filter(e -> deleted.contains(e.s3Key())).map(Entry::id).toList();
        queue.remove(done);
        return done.size() == BATCH;
    }
}