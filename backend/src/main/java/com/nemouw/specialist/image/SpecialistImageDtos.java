package com.nemouw.specialist.image;

public final class SpecialistImageDtos {
    private SpecialistImageDtos() {
    }

    public record ImageResponse(String url, String mimeType) {
    }
}