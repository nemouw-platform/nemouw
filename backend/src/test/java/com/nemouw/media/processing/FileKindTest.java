package com.nemouw.media.processing;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.stream.Stream;

import static com.nemouw.media.processing.ImageFixtures.ascii;
import static com.nemouw.media.processing.ImageFixtures.bytes;
import static com.nemouw.media.processing.ImageFixtures.concat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class FileKindTest {

    private static byte[] webpHeader(String chunkTag) {
        return concat(ascii("RIFF"), bytes(0x24, 0, 0, 0), ascii("WEBP"), ascii(chunkTag));
    }

    static Stream<Arguments> realisticHeaders() {
        return Stream.of(
                arguments("JPEG with a JFIF segment",
                        concat(bytes(0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10), ascii("JFIF")), FileKind.JPEG),
                arguments("PNG with its first chunk",
                        concat(bytes(0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13), ascii("IHDR")),
                        FileKind.PNG),
                arguments("PDF 1.7", ascii("%PDF-1.7\n1 0 obj"), FileKind.PDF),
                arguments("lossy WebP", webpHeader("VP8 "), FileKind.WEBP));
    }

    static Stream<Arguments> unrecognised() {
        return Stream.of(
                arguments("GIF", ascii("GIF89a")),
                arguments("BMP", ascii("BM")),
                arguments("TIFF", bytes('I', 'I', 0x2A, 0x00)),
                arguments("HEIC", concat(bytes(0, 0, 0, 0x18), ascii("ftypheic"))),
                arguments("SVG text", ascii("<svg xmlns=\"http://www.w3.org/2000/svg\">")),
                arguments("ZIP", ascii("PK")),
                arguments("WAV, a RIFF file that is not WebP", concat(ascii("RIFF"), bytes(0, 0, 0, 0), ascii("WAVE"))),
                arguments("AVI, a RIFF file that is not WebP", concat(ascii("RIFF"), bytes(0, 0, 0, 0), ascii("AVI "))),
                arguments("PDF header without the dash", ascii("%PDF1.7")),
                arguments("PDF header after a byte order mark", concat(bytes(0xEF, 0xBB, 0xBF), ascii("%PDF-1.7"))),
                arguments("PDF header after a space", ascii(" %PDF-1.7")),
                arguments("an empty file", new byte[0]),
                arguments("a single byte", bytes(0xFF)));
    }

    @Nested
    class HappyPaths {

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.nemouw.media.processing.FileKindTest#realisticHeaders")
        void detectsEveryKindFromARealisticHeader(String label, byte[] header, FileKind expected) {
            assertThat(FileKind.detect(header)).contains(expected);
        }

        @ParameterizedTest
        @ValueSource(strings = {"VP8 ", "VP8L", "VP8X"})
        void detectsEveryWebpChunkType(String chunkTag) {
            assertThat(FileKind.detect(webpHeader(chunkTag))).contains(FileKind.WEBP);
        }

        @Test
        void looksOnlyAtTheFirstTwelveBytesOfAWebpSoABrokenBodyIsLeftToTheImageProcessor() {
            assertThat(FileKind.detect(webpHeader("ZZZZ"))).contains(FileKind.WEBP);
        }

        @Test
        void describesEveryKindWithItsMimeTypeAndExtension() {
            assertThat(Arrays.stream(FileKind.values()).map(FileKind::mimeType))
                    .containsExactly("image/jpeg", "image/png", "image/webp", "application/pdf");
            assertThat(Arrays.stream(FileKind.values()).map(FileKind::extension))
                    .containsExactly("jpg", "png", "webp", "pdf");
        }
    }

    @Nested
    class BoundaryValues {

        @Test
        void acceptsTheShortestHeaderOfEachKindAndRejectsOneByteLess() {
            byte[] jpeg = bytes(0xFF, 0xD8, 0xFF);
            byte[] png = bytes(0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A);
            byte[] pdf = ascii("%PDF-");
            byte[] webp = webpHeader("");

            assertThat(webp).hasSize(12);
            assertThat(FileKind.detect(jpeg)).contains(FileKind.JPEG);
            assertThat(FileKind.detect(Arrays.copyOf(jpeg, 2))).isEmpty();
            assertThat(FileKind.detect(png)).contains(FileKind.PNG);
            assertThat(FileKind.detect(Arrays.copyOf(png, 7))).isEmpty();
            assertThat(FileKind.detect(pdf)).contains(FileKind.PDF);
            assertThat(FileKind.detect(Arrays.copyOf(pdf, 4))).isEmpty();
            assertThat(FileKind.detect(webp)).contains(FileKind.WEBP);
            assertThat(FileKind.detect(Arrays.copyOf(webp, 11))).isEmpty();
        }

        @Test
        void hasExactlyFourKinds() {
            assertThat(FileKind.values()).hasSize(4);
        }
    }

    @Nested
    class ErrorPaths {

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.nemouw.media.processing.FileKindTest#unrecognised")
        void doesNotRecogniseOtherFormats(String label, byte[] header) {
            assertThat(FileKind.detect(header)).isEmpty();
        }

        @Test
        void returnsEmptyForNull() {
            assertThat(FileKind.detect(null)).isEmpty();
        }
    }
}