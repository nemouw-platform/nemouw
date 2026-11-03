package com.nemouw.media.api;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

/** What an upload is for. Chosen by the endpoint, never by the client. */
public enum MediaKind {
    PROFILE_PICTURE("profile-pictures", true, false),
    COVER_IMAGE("cover-images", true, false),
    CREDENTIAL_DOCUMENT("credential-documents", false, true);

    private final String prefix;
    private final boolean publicAccess;
    private final boolean allowsPdf;

    MediaKind(String prefix, boolean publicAccess, boolean allowsPdf) {
        this.prefix = prefix;
        this.publicAccess = publicAccess;
        this.allowsPdf = allowsPdf;
    }

    public boolean isPublic() {
        return publicAccess;
    }

    public boolean allowsPdf() {
        return allowsPdf;
    }

    public String cacheControl() {
        return publicAccess ? "public, max-age=31536000, immutable" : "private, no-store";
    }

    public String newKey(String extension) {
        return prefix + "/" + UUID.randomUUID() + "." + extension;
    }

    public static Optional<MediaKind> ofKey(String key) {
        return Arrays.stream(values()).filter(k -> key.startsWith(k.prefix + "/")).findFirst();
    }
}