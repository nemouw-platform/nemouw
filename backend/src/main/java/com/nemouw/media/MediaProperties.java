package com.nemouw.media;

import com.nemouw.media.api.MediaKind;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.media")
public record MediaProperties(
        @NotBlank String bucket,
        @NotBlank String region,
        @NotBlank String publicBaseUrl,
        String endpoint,
        @NotNull Duration presignedTtl,
        @Valid @NotNull Limits profilePicture,
        @Valid @NotNull Limits coverImage,
        @Valid @NotNull Limits credentialDocument,
        @Valid @NotNull Processing processing) {

    public record Limits(@NotNull DataSize maxSize, @Min(1) int width, @Min(1) int height,
                         boolean crop, @Min(1) @Max(100) int quality) {
    }

    public record Processing(@Min(1) int threads, @Min(1) int queueCapacity, @NotNull Duration timeout,
                             @Min(1) long maxPixels) {
    }

    public Limits limitsOf(MediaKind kind) {
        return switch (kind) {
            case PROFILE_PICTURE -> profilePicture;
            case COVER_IMAGE -> coverImage;
            case CREDENTIAL_DOCUMENT -> credentialDocument;
        };
    }

    public boolean hasEndpoint() {
        return endpoint != null && !endpoint.isBlank();
    }
}