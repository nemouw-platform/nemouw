package com.nemouw.media.api;

import com.nemouw.common.exception.ApiException;
import com.nemouw.common.exception.ErrorCode;
import com.nemouw.common.exception.Errors;
import com.nemouw.common.exception.FieldErrors;
import com.nemouw.common.exception.ValidationException;
import com.nemouw.media.MediaProperties;
import com.nemouw.media.MediaProperties.Limits;
import com.nemouw.media.processing.FileKind;
import com.nemouw.media.processing.ImageProcessor;
import com.nemouw.media.processing.PdfValidator;
import com.nemouw.media.storage.MediaRepository;
import com.nemouw.media.storage.MediaRow;
import com.nemouw.media.storage.ObjectStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Uploads in two steps so no database transaction is held open during image work:
 * {@link #upload} validates, normalizes and stores the file; {@link #register} records it,
 * inside the caller's transaction, together with the foreign key that links it.
 */
@Service
public class MediaStore {
    private static final Logger log = LoggerFactory.getLogger(MediaStore.class);

    private final MediaProperties props;
    private final ImageProcessor images;
    private final ObjectStorage storage;
    private final MediaRepository media;
    private final PublicMediaUrls urls;

    MediaStore(MediaProperties props, ImageProcessor images, ObjectStorage storage,
               MediaRepository media, PublicMediaUrls urls) {
        this.props = props;
        this.images = images;
        this.storage = storage;
        this.media = media;
        this.urls = urls;
    }

    public UploadedObject upload(MediaKind kind, MultipartFile file) {
        Limits limits = props.limitsOf(kind);
        byte[] source = read(file, limits);
        FileKind type = FileKind.detect(source)
                .filter(t -> t != FileKind.PDF || kind.allowsPdf())
                .orElseThrow(Errors::unsupportedFileType);

        boolean pdf = type == FileKind.PDF;
        byte[] content = pdf ? PdfValidator.validated(source) : images.toWebp(source, limits);
        FileKind output = pdf ? FileKind.PDF : FileKind.WEBP;

        String key = kind.newKey(output.extension());
        storage.put(key, output.mimeType(), kind.cacheControl(), content);
        return new UploadedObject(kind, key, output.mimeType());
    }

    /** Deletes the object if the surrounding transaction rolls back. */
    @Transactional(propagation = Propagation.MANDATORY)
    public StoredMedia register(UploadedObject uploaded) {
        discardOnRollback(uploaded.key());
        MediaRow row = media.insert(uploaded.key(), uploaded.mimeType());
        String url = uploaded.kind().isPublic() ? urls.urlFor(row.s3Key()) : null;
        return new StoredMedia(row.id(), url, row.mimeType(), row.createdAt().toInstant());
    }

    /** The caller must have authorized access to this media. Private kinds only. */
    public PresignedLink presign(UUID mediaId) {
        String key = media.findKey(mediaId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (MediaKind.ofKey(key).filter(MediaKind::isPublic).isPresent()) {
            throw new IllegalArgumentException("Media is public: " + mediaId);
        }
        Duration ttl = props.presignedTtl();
        return new PresignedLink(storage.presignGet(key, ttl), Instant.now().plus(ttl));
    }

    private static byte[] read(MultipartFile file, Limits limits) {
        if (file == null || file.isEmpty()) throw new ValidationException(List.of(FieldErrors.required("file")));
        if (file.getSize() > limits.maxSize().toBytes()) throw Errors.fileTooLarge();
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw Errors.invalidField("file", "the file could not be read");
        }
    }

    private void discardOnRollback(String key) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) discard(key);
            }
        });
    }

    private void discard(String key) {
        try {
            storage.delete(List.of(key));
        } catch (RuntimeException e) {
            log.error("MEDIA_ORPHANED key={} cause={}", key, e.toString());
        }
    }
}