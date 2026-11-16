package com.nemouw.media.processing;

import com.nemouw.common.exception.FieldError;
import com.nemouw.common.exception.FieldErrors;
import com.nemouw.common.exception.ValidationException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static com.nemouw.media.processing.ImageFixtures.ascii;
import static com.nemouw.media.processing.ImageFixtures.bytes;
import static com.nemouw.media.processing.ImageFixtures.concat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class PdfValidatorTest {

    private static final FieldError UNREADABLE = FieldErrors.invalidFormat("file", "the file could not be read");

    private static byte[] pdf(int bodyLength, String trailer) {
        return concat(ascii("%PDF-1.7\n"), new byte[bodyLength], ascii("%%EOF"), ascii(trailer));
    }

    private static List<FieldError> errorsOf(ThrowingCallable call) {
        Throwable thrown = catchThrowable(call);
        assertThat(thrown).isInstanceOf(ValidationException.class);
        return ((ValidationException) thrown).fieldErrors();
    }

    @Nested
    class HappyPaths {

        @Test
        void returnsTheSameArrayWhenTheEndMarkerIsPresent() {
            byte[] pdf = pdf(10, "\n");

            assertThat(PdfValidator.validated(pdf)).isSameAs(pdf);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "\n", "\r\n", " ", "\r\n\r\n   "})
        void acceptsLineEndingsAndSpacesAfterTheMarker(String trailer) {
            byte[] pdf = pdf(10, trailer);

            assertThat(PdfValidator.validated(pdf)).isSameAs(pdf);
        }

        @Test
        void acceptsAFileShorterThanTheWindow() {
            byte[] pdf = concat(ascii("%PDF-1.4 %%EOF"));

            assertThat(PdfValidator.validated(pdf)).isSameAs(pdf);
        }

        @Test
        void acceptsBinaryBytesAroundTheMarker() {
            byte[] pdf = concat(new byte[500], ascii("%%EOF"), bytes(0x80, 0xFF, 0xFE, 0x00));

            assertThat(PdfValidator.validated(pdf)).isSameAs(pdf);
        }

        @Test
        void acceptsAnIncrementalUpdateWhoseEarlierMarkerIsOutsideTheWindow() {
            byte[] pdf = concat(ascii("%PDF-1.7\n%%EOF\n"), new byte[2000], ascii("update"), ascii("%%EOF\r\n"));

            assertThat(PdfValidator.validated(pdf)).isSameAs(pdf);
        }

        @Test
        void doesNotCheckTheHeaderBecauseFileKindDoesThat() {
            byte[] notReallyAPdf = concat(ascii("garbage"), ascii("%%EOF"));

            assertThat(PdfValidator.validated(notReallyAPdf)).isSameAs(notReallyAPdf);
        }

        @Test
        void acceptsATruncatedUpdateShorterThanTheWindowBecauseTheOldMarkerIsStillInIt() {
            byte[] pdf = concat(ascii("%PDF-1.7\n"), new byte[2000], ascii("%%EOF\n"), new byte[300]);

            assertThat(PdfValidator.validated(pdf)).isSameAs(pdf);
        }
    }

    @Nested
    class BoundaryValues {

        @Test
        void acceptsAMarkerThatStartsExactlyAtTheStartOfTheWindow() {
            byte[] pdf = pdf(10, " ".repeat(1019));

            assertThat(PdfValidator.validated(pdf)).isSameAs(pdf);
        }

        @Test
        void rejectsAMarkerThatStartsOneByteBeforeTheWindow() {
            byte[] pdf = pdf(10, " ".repeat(1020));

            assertThat(errorsOf(() -> PdfValidator.validated(pdf))).containsExactly(UNREADABLE);
        }

        @Test
        void acceptsA1024ByteFileThatStartsWithTheMarker() {
            byte[] pdf = concat(ascii("%%EOF"), new byte[1019]);

            assertThat(PdfValidator.validated(pdf)).isSameAs(pdf);
        }
    }

    @Nested
    class ErrorPaths {

        @Test
        void rejectsAFileWithoutTheMarker() {
            byte[] pdf = concat(ascii("%PDF-1.7\n"), new byte[3000]);

            assertThat(errorsOf(() -> PdfValidator.validated(pdf))).containsExactly(UNREADABLE);
        }

        @Test
        void rejectsABigFileWhoseOnlyMarkerIsNearTheStart() {
            byte[] pdf = concat(ascii("%PDF-1.7\n%%EOF\n"), new byte[2000]);

            assertThat(errorsOf(() -> PdfValidator.validated(pdf))).containsExactly(UNREADABLE);
        }

        @Test
        void rejectsAnIncrementalUpdateThatWasCutOffAfterTheWindow() {
            byte[] pdf = concat(ascii("%PDF-1.7\n%%EOF\n"), ascii("update"), new byte[1500]);

            assertThat(errorsOf(() -> PdfValidator.validated(pdf))).containsExactly(UNREADABLE);
        }

        @Test
        void rejectsAnEmptyFile() {
            assertThat(errorsOf(() -> PdfValidator.validated(new byte[0]))).containsExactly(UNREADABLE);
        }

        @ParameterizedTest
        @ValueSource(strings = {"%%EO", "%EOF", "%%eof", "%%E\nOF", "EOF%%"})
        void rejectsAnythingThatIsNotExactlyTheMarker(String text) {
            assertThat(errorsOf(() -> PdfValidator.validated(ascii(text)))).containsExactly(UNREADABLE);
        }

        @Test
        void rejectsNullWithTheSameError() {
            assertThat(errorsOf(() -> PdfValidator.validated(null))).containsExactly(UNREADABLE);
        }
    }
}