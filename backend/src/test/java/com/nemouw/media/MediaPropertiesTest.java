package com.nemouw.media;

import com.nemouw.media.MediaProperties.Limits;
import com.nemouw.media.MediaProperties.Processing;
import com.nemouw.media.api.MediaKind;
import com.nemouw.support.ShippedProperties;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.util.unit.DataSize;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class MediaPropertiesTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
    private static final List<String> SLOTS = List.of("profilePicture", "coverImage", "credentialDocument");
    private static final long MAX_PIXELS = 25_000_000L;
    private static final Duration TTL = Duration.ofMinutes(5);
    private static final Limits GOOD = limits(512, 512, 80);
    private static final Processing PROCESSING = processing(2, 4, MAX_PIXELS);

    private static Limits limits(int width, int height, int quality) {
        return new Limits(DataSize.ofMegabytes(2), width, height, true, quality);
    }

    private static Processing processing(int threads, int queueCapacity, long maxPixels) {
        return new Processing(threads, queueCapacity, Duration.ofSeconds(15), maxPixels);
    }

    private static MediaProperties valid() {
        return new MediaProperties("nemouw-media", "eu-central-1", "https://cdn.nemouw.test", null, TTL,
                GOOD, GOOD, GOOD, PROCESSING);
    }

    private static MediaProperties withText(String bucket, String region, String publicBaseUrl, String endpoint) {
        return new MediaProperties(bucket, region, publicBaseUrl, endpoint, TTL, GOOD, GOOD, GOOD, PROCESSING);
    }

    private static MediaProperties withSlot(String slot, Limits limits) {
        return switch (slot) {
            case "profilePicture" -> new MediaProperties("b", "r", "u", null, TTL, limits, GOOD, GOOD, PROCESSING);
            case "coverImage" -> new MediaProperties("b", "r", "u", null, TTL, GOOD, limits, GOOD, PROCESSING);
            case "credentialDocument" -> new MediaProperties("b", "r", "u", null, TTL, GOOD, GOOD, limits, PROCESSING);
            default -> throw new IllegalArgumentException(slot);
        };
    }

    private static MediaProperties withProcessing(Processing processing) {
        return new MediaProperties("b", "r", "u", null, TTL, GOOD, GOOD, GOOD, processing);
    }

    private static Set<String> invalidProperties(MediaProperties properties) {
        return VALIDATOR.validate(properties).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    private static Stream<Arguments> slotsWith(int... values) {
        return SLOTS.stream().flatMap(slot -> Arrays.stream(values).mapToObj(value -> arguments(slot, value)));
    }

    static Stream<Arguments> invalidSizes() {
        return slotsWith(0, -1);
    }

    static Stream<Arguments> invalidQualities() {
        return slotsWith(0, -1, 101, 1000);
    }

    static Stream<Arguments> validQualities() {
        return slotsWith(1, 100);
    }

    @Nested
    class HappyPaths {

        @Test
        void acceptsAValidConfiguration() {
            assertThat(invalidProperties(valid())).isEmpty();
        }

        @ParameterizedTest
        @MethodSource("com.nemouw.media.MediaPropertiesTest#validQualities")
        void acceptsTheLowestAndHighestQuality(String slot, int quality) {
            assertThat(invalidProperties(withSlot(slot, limits(512, 512, quality)))).isEmpty();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "http://localhost:9000"})
        void neverValidatesTheOptionalEndpoint(String endpoint) {
            assertThat(invalidProperties(withText("b", "r", "u", endpoint))).isEmpty();
        }

        @Test
        void returnsTheLimitsOfEachKind() {
            Limits profile = limits(1, 1, 10);
            Limits cover = limits(2, 2, 20);
            Limits credential = limits(3, 3, 30);
            MediaProperties properties =
                    new MediaProperties("b", "r", "u", null, TTL, profile, cover, credential, PROCESSING);

            assertThat(properties.limitsOf(MediaKind.PROFILE_PICTURE)).isSameAs(profile);
            assertThat(properties.limitsOf(MediaKind.COVER_IMAGE)).isSameAs(cover);
            assertThat(properties.limitsOf(MediaKind.CREDENTIAL_DOCUMENT)).isSameAs(credential);
        }

        @Test
        void reportsAnEndpointOnlyWhenOneIsSet() {
            assertThat(withText("b", "r", "u", "http://localhost:9000").hasEndpoint()).isTrue();
        }
    }

    @Nested
    class BoundaryValues {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        void reportsNoEndpointForNullEmptyAndBlank(String endpoint) {
            assertThat(withText("b", "r", "u", endpoint).hasEndpoint()).isFalse();
        }

        @ParameterizedTest
        @MethodSource("com.nemouw.media.MediaPropertiesTest#invalidSizes")
        void rejectsAZeroOrNegativeWidth(String slot, int width) {
            assertThat(invalidProperties(withSlot(slot, limits(width, 512, 80)))).containsExactly(slot + ".width");
        }

        @ParameterizedTest
        @MethodSource("com.nemouw.media.MediaPropertiesTest#invalidSizes")
        void rejectsAZeroOrNegativeHeight(String slot, int height) {
            assertThat(invalidProperties(withSlot(slot, limits(512, height, 80)))).containsExactly(slot + ".height");
        }

        @ParameterizedTest
        @MethodSource("com.nemouw.media.MediaPropertiesTest#invalidQualities")
        void rejectsAQualityOutsideOneToOneHundred(String slot, int quality) {
            assertThat(invalidProperties(withSlot(slot, limits(512, 512, quality))))
                    .containsExactly(slot + ".quality");
        }

        @ParameterizedTest
        @CsvSource({
                "threads,       0",
                "threads,       -1",
                "queueCapacity, 0",
                "queueCapacity, -1",
                "maxPixels,     0",
                "maxPixels,     -1"
        })
        void rejectsAZeroOrNegativeProcessingLimit(String field, long value) {
            Processing bad = switch (field) {
                case "threads" -> processing((int) value, 4, MAX_PIXELS);
                case "queueCapacity" -> processing(2, (int) value, MAX_PIXELS);
                case "maxPixels" -> processing(2, 4, value);
                default -> throw new IllegalArgumentException(field);
            };

            assertThat(invalidProperties(withProcessing(bad))).containsExactly("processing." + field);
        }
    }

    @Nested
    class ErrorPaths {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        void requiresABucket(String bucket) {
            assertThat(invalidProperties(withText(bucket, "r", "u", null))).containsExactly("bucket");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        void requiresARegion(String region) {
            assertThat(invalidProperties(withText("b", region, "u", null))).containsExactly("region");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        void requiresAPublicBaseUrl(String url) {
            assertThat(invalidProperties(withText("b", "r", url, null))).containsExactly("publicBaseUrl");
        }

        @Test
        void requiresAPresignedTtl() {
            MediaProperties properties = new MediaProperties("b", "r", "u", null, null, GOOD, GOOD, GOOD, PROCESSING);

            assertThat(invalidProperties(properties)).containsExactly("presignedTtl");
        }

        @ParameterizedTest
        @ValueSource(strings = {"profilePicture", "coverImage", "credentialDocument"})
        void requiresEveryLimitsObject(String slot) {
            assertThat(invalidProperties(withSlot(slot, null))).containsExactly(slot);
        }

        @ParameterizedTest
        @ValueSource(strings = {"profilePicture", "coverImage", "credentialDocument"})
        void requiresAMaximumSizeOnEveryLimitsObject(String slot) {
            Limits withoutSize = new Limits(null, 512, 512, true, 80);

            assertThat(invalidProperties(withSlot(slot, withoutSize))).containsExactly(slot + ".maxSize");
        }

        @Test
        void requiresTheProcessingObjectAndItsTimeout() {
            assertThat(invalidProperties(withProcessing(null))).containsExactly("processing");
            assertThat(invalidProperties(withProcessing(new Processing(2, 4, null, MAX_PIXELS))))
                    .containsExactly("processing.timeout");
        }
    }

    @Nested
    class ShippedConfiguration {

        private final MediaProperties shipped = ShippedProperties.bind("app.media", MediaProperties.class);

        @Test
        void onlyLacksTheValuesThatComeFromTheEnvironment() {
            assertThat(invalidProperties(shipped)).containsExactlyInAnyOrder("bucket", "region", "publicBaseUrl");
        }

        @Test
        void cropsPicturesButNeverCredentialDocuments() {
            assertThat(shipped.limitsOf(MediaKind.PROFILE_PICTURE).crop()).isTrue();
            assertThat(shipped.limitsOf(MediaKind.COVER_IMAGE).crop()).isTrue();
            assertThat(shipped.limitsOf(MediaKind.CREDENTIAL_DOCUMENT).crop()).isFalse();
        }
    }
}