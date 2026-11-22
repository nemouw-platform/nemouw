package com.nemouw.media.api;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * What an upload is for. Chosen by the endpoint, never by the client.
 */
public enum MediaKind {
    PROFILE_PICTURE("profile-pictures", true, false),
    COVER_IMAGE("cover-images", true, false),
    CREDENTIAL_DOCUMENT("credential-documents", false, true);
    private static final Pattern FILE_NAME =
            Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.[a-z0-9]+");

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

    /**
     * The inverse of {@link #newKey}: only keys of exactly that shape belong to a kind.
     */
    public static Optional<MediaKind> ofKey(String key) {
        if (key == null) return Optional.empty();
        return Arrays.stream(values())
                .filter(k -> key.startsWith(k.prefix + "/")
                        && FILE_NAME.matcher(key.substring(k.prefix.length() + 1)).matches())
                .findFirst();
    }
}