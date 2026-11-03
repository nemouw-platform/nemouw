package com.nemouw.specialist.image;

import com.nemouw.media.api.MediaKind;
import com.nemouw.media.api.MediaStore;
import com.nemouw.media.api.StoredMedia;
import com.nemouw.media.api.UploadedObject;
import com.nemouw.specialist.image.SpecialistImageDtos.ImageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
class SpecialistImageService {
    private final MediaStore media;
    private final SpecialistImageRepository images;
    private final TransactionTemplate tx;

    SpecialistImageService(MediaStore media, SpecialistImageRepository images, TransactionTemplate tx) {
        this.media = media;
        this.images = images;
        this.tx = tx;
    }

    ImageResponse replace(UUID specialistId, MediaKind kind, MultipartFile file) {
        UploadedObject uploaded = media.upload(kind, file);
        StoredMedia stored = tx.execute(status -> {
            StoredMedia registered = media.register(uploaded);
            images.link(specialistId, kind, registered.id());
            return registered;
        });
        return new ImageResponse(stored.url(), stored.mimeType());
    }

    void remove(UUID specialistId, MediaKind kind) {
        images.unlink(specialistId, kind);
    }
}