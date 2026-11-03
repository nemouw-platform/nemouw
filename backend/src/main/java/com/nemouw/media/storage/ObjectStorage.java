package com.nemouw.media.storage;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public interface ObjectStorage {
    void put(String key, String contentType, String cacheControl, byte[] content);

    /** Returns the keys now gone; a missing key counts as gone. */
    Set<String> delete(Collection<String> keys);

    String presignGet(String key, Duration ttl);

    /** Hands over keys of objects last modified before the cutoff, in batches of up to 1000. */
    void scanOlderThan(Instant cutoff, Consumer<List<String>> batch);
}