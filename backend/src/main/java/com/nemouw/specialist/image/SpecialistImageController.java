package com.nemouw.specialist.image;

import com.nemouw.media.api.MediaKind;
import com.nemouw.specialist.image.SpecialistImageDtos.ImageResponse;
import com.nemouw.specialist.profile.CurrentSpecialistId;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/specialists/me")
public class SpecialistImageController {
    private final SpecialistImageService service;

    SpecialistImageController(SpecialistImageService service) {
        this.service = service;
    }

    @PutMapping(value = "/profile-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ImageResponse setProfilePicture(@CurrentSpecialistId UUID specialistId,
                                    @RequestPart(name = "file", required = false) MultipartFile file) {
        return service.replace(specialistId, MediaKind.PROFILE_PICTURE, file);
    }

    @DeleteMapping("/profile-picture")
    ResponseEntity<Void> deleteProfilePicture(@CurrentSpecialistId UUID specialistId) {
        service.remove(specialistId, MediaKind.PROFILE_PICTURE);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/cover-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ImageResponse setCoverImage(@CurrentSpecialistId UUID specialistId,
                                @RequestPart(name = "file", required = false) MultipartFile file) {
        return service.replace(specialistId, MediaKind.COVER_IMAGE, file);
    }

    @DeleteMapping("/cover-image")
    ResponseEntity<Void> deleteCoverImage(@CurrentSpecialistId UUID specialistId) {
        service.remove(specialistId, MediaKind.COVER_IMAGE);
        return ResponseEntity.noContent().build();
    }
}