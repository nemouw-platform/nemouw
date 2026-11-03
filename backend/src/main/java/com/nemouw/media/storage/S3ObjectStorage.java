package com.nemouw.media.storage;

import com.nemouw.media.MediaProperties;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeletedObject;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component
class S3ObjectStorage implements ObjectStorage {
    private static final int PAGE = 1000;

    private final S3Client s3;
    private final S3Presigner presigner;
    private final String bucket;

    S3ObjectStorage(S3Client s3, S3Presigner presigner, MediaProperties props) {
        this.s3 = s3;
        this.presigner = presigner;
        this.bucket = props.bucket();
    }

    @Override
    public void put(String key, String contentType, String cacheControl, byte[] content) {
        s3.putObject(PutObjectRequest.builder()
                        .bucket(bucket).key(key).contentType(contentType).cacheControl(cacheControl).build(),
                RequestBody.fromBytes(content));
    }

    @Override
    public Set<String> delete(Collection<String> keys) {
        if (keys.isEmpty()) return Set.of();
        var objects = keys.stream().map(k -> ObjectIdentifier.builder().key(k).build()).toList();
        return s3.deleteObjects(DeleteObjectsRequest.builder()
                        .bucket(bucket)
                        .delete(Delete.builder().objects(objects).quiet(false).build())
                        .build())
                .deleted().stream()
                .map(DeletedObject::key)
                .collect(Collectors.toSet());
    }

    @Override
    public String presignGet(String key, Duration ttl) {
        return presigner.presignGetObject(r -> r
                        .signatureDuration(ttl)
                        .getObjectRequest(g -> g.bucket(bucket).key(key)))
                .url().toString();
    }

    @Override
    public void scanOlderThan(Instant cutoff, Consumer<List<String>> batch) {
        List<String> page = new ArrayList<>();
        for (S3Object object : s3.listObjectsV2Paginator(r -> r.bucket(bucket)).contents()) {
            if (!object.lastModified().isBefore(cutoff)) continue;
            page.add(object.key());
            if (page.size() == PAGE) {
                batch.accept(page);
                page = new ArrayList<>();
            }
        }
        if (!page.isEmpty()) batch.accept(page);
    }
}