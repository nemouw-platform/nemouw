package com.nemouw.media.api;

import java.time.Instant;

public record PresignedLink(String url, Instant expiresAt) {
}