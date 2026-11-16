package com.nemouw.media.api;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class MediaKindTest {

    private static final String ID = "0b8f6c9e-3b1a-4c53-9a2d-7e5f1c4d8a60";
    private static final String UPPERCASE_ID = "0B8F6C9E-3B1A-4C53-9A2D-7E5F1C4D8A60";

    @Nested
    class HappyPaths {

        @ParameterizedTest
        @CsvSource({
                "PROFILE_PICTURE,     profile-pictures",
                "COVER_IMAGE,         cover-images",
                "CREDENTIAL_DOCUMENT, credential-documents"
        })
        void putsEveryKeyUnderThePrefixOfItsKind(MediaKind kind, String prefix) {
            assertThat(kind.newKey("webp")).startsWith(prefix + "/").endsWith(".webp");
        }

        @ParameterizedTest
        @EnumSource(MediaKind.class)
        void namesTheFileAfterAUuid(MediaKind kind) {
            String key = kind.newKey("webp");
            String fileName = key.substring(key.indexOf('/') + 1);

            assertThatCode(() -> UUID.fromString(fileName.substring(0, fileName.length() - ".webp".length())))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @EnumSource(MediaKind.class)
        void generatesADifferentKeyEveryTime(MediaKind kind) {
            assertThat(kind.newKey("webp")).isNotEqualTo(kind.newKey("webp"));
        }

        @ParameterizedTest
        @EnumSource(MediaKind.class)
        void ofKeyIsTheInverseOfNewKey(MediaKind kind) {
            for (String extension : new String[]{"webp", "pdf", "jpg", "png"}) {
                assertThat(MediaKind.ofKey(kind.newKey(extension))).contains(kind);
            }
        }

        @Test
        void makesPicturesPublicAndDocumentsPrivate() {
            assertThat(MediaKind.PROFILE_PICTURE.isPublic()).isTrue();
            assertThat(MediaKind.COVER_IMAGE.isPublic()).isTrue();
            assertThat(MediaKind.CREDENTIAL_DOCUMENT.isPublic()).isFalse();
        }

        @Test
        void allowsPdfOnlyForCredentialDocuments() {
            assertThat(MediaKind.PROFILE_PICTURE.allowsPdf()).isFalse();
            assertThat(MediaKind.COVER_IMAGE.allowsPdf()).isFalse();
            assertThat(MediaKind.CREDENTIAL_DOCUMENT.allowsPdf()).isTrue();
        }

        @Test
        void cachesPublicFilesForeverAndNeverStoresPrivateOnes() {
            assertThat(MediaKind.PROFILE_PICTURE.cacheControl()).isEqualTo("public, max-age=31536000, immutable");
            assertThat(MediaKind.COVER_IMAGE.cacheControl()).isEqualTo("public, max-age=31536000, immutable");
            assertThat(MediaKind.CREDENTIAL_DOCUMENT.cacheControl()).isEqualTo("private, no-store");
        }
    }

    @Nested
    class BoundaryValues {

        @Test
        void hasExactlyThreeKinds() {
            assertThat(MediaKind.values()).hasSize(3);
        }
    }

    @Nested
    class ErrorPaths {

        @ParameterizedTest
        @ValueSource(strings = {
                "profile-pictures/../secret.webp",
                "profile-pictures/sub/" + ID + ".webp",
                "cover-images/" + ID + ".webp/extra",
                "profile-pictures/" + ID,
                "profile-pictures/" + ID + ".",
                "profile-pictures/" + ID + ".webp\n",
                "profile-pictures/" + ID + ".WEBP",
                "profile-pictures/" + UPPERCASE_ID + ".webp",
                "profile-pictures/short.webp",
                "/profile-pictures/" + ID + ".webp",
                "Profile-Pictures/" + ID + ".webp",
                "profile-pictures-evil/" + ID + ".webp",
                "profile-pictures",
                "profile-pictures/"
        })
        void doesNotClassifyKeysThatNewKeyCouldNeverHaveGenerated(String key) {
            assertThat(MediaKind.ofKey(key)).isEmpty();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        void returnsEmptyForNullEmptyAndBlank(String key) {
            assertThat(MediaKind.ofKey(key)).isEmpty();
        }
    }
}