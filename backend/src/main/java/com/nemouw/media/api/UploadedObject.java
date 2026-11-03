package com.nemouw.media.api;

/** An object already in S3 but not yet recorded in the database. */
public record UploadedObject(MediaKind kind, String key, String mimeType) {
}