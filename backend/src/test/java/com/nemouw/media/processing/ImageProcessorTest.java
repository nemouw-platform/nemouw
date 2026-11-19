package com.nemouw.media.processing;

import com.nemouw.common.exception.ErrorCode;
import com.nemouw.common.exception.FieldError;
import com.nemouw.common.exception.FieldErrors;
import com.nemouw.common.exception.RetryAfterException;
import com.nemouw.common.exception.ValidationException;
import com.nemouw.media.MediaProperties.Limits;
import com.nemouw.media.MediaProperties.Processing;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.util.unit.DataSize;

import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.nemouw.media.processing.ImageFixtures.concat;
import static com.nemouw.media.processing.ImageFixtures.bytes;
import static com.nemouw.media.processing.ImageFixtures.contains;
import static com.nemouw.media.processing.ImageFixtures.decode;
import static com.nemouw.media.processing.ImageFixtures.encode;
import static com.nemouw.media.processing.ImageFixtures.exifOrientation;
import static com.nemouw.media.processing.ImageFixtures.jpeg;
import static com.nemouw.media.processing.ImageFixtures.jpegWithExif;
import static com.nemouw.media.processing.ImageFixtures.noiseJpeg;
import static com.nemouw.media.processing.ImageFixtures.png;
import static com.nemouw.media.processing.ImageFixtures.pngHeaderOnly;
import static com.nemouw.media.processing.ImageFixtures.randomBytes;
import static com.nemouw.media.processing.ImageFixtures.transparentPng;
import static com.nemouw.media.processing.ImageFixtures.webp;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class ImageProcessorTest {

    private static final long MAX_PIXELS = 25_000_000L;
    private static final FieldError UNREADABLE = FieldErrors.invalidFormat("file", "the file could not be read");
    private static final FieldError TOO_LARGE =
            FieldErrors.invalidFormat("file", "the image dimensions are too large");
    private static final String SECRET = "SECRET-GPS-MARKER";

    private final List<ImageProcessor> processors = new ArrayList<>();

    @AfterEach
    void shutDownProcessorsAndClearTheInterruptFlag() {
        processors.forEach(ImageProcessor::shutdown);
        Thread.interrupted();
    }

    private ImageProcessor processor() {
        return processor(2, 4, Duration.ofSeconds(30), MAX_PIXELS);
    }

    private ImageProcessor processor(long maxPixels) {
        return processor(2, 4, Duration.ofSeconds(30), maxPixels);
    }

    private ImageProcessor processor(int threads, int queue, Duration timeout, long maxPixels) {
        return track(new ImageProcessor(new Processing(threads, queue, timeout, maxPixels), pool(threads, queue)));
    }

    private ImageProcessor track(ImageProcessor processor) {
        processors.add(processor);
        return processor;
    }

    private static ThreadPoolExecutor pool(int threads, int queue) {
        return new ThreadPoolExecutor(threads, threads, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queue), new ThreadPoolExecutor.AbortPolicy());
    }

    private static Limits crop(int width, int height, int quality) {
        return new Limits(DataSize.ofMegabytes(10), width, height, true, quality);
    }

    private static Limits fit(int width, int height, int quality) {
        return new Limits(DataSize.ofMegabytes(10), width, height, false, quality);
    }

    private static BufferedImage webpOf(byte[] output) {
        assertThat(FileKind.detect(output)).contains(FileKind.WEBP);
        return decode(output);
    }

    private static void assertSize(byte[] output, int width, int height) {
        BufferedImage image = webpOf(output);
        assertThat(image.getWidth()).isEqualTo(width);
        assertThat(image.getHeight()).isEqualTo(height);
    }

    private static List<FieldError> errorsOf(ThrowingCallable call) {
        Throwable thrown = catchThrowable(call);
        assertThat(thrown).isInstanceOf(ValidationException.class);
        return ((ValidationException) thrown).fieldErrors();
    }

    private static void assertBusy(Throwable thrown) {
        assertThat(thrown).isInstanceOf(RetryAfterException.class);
        RetryAfterException busy = (RetryAfterException) thrown;
        assertThat(busy.errorCode()).isEqualTo(ErrorCode.MEDIA_BUSY);
        assertThat(busy.retryAfterSeconds()).isEqualTo(3);
        assertThat(busy.responseHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("3");
    }

    private static void assertBusy(ThrowingCallable call) {
        assertBusy(catchThrowable(call));
    }

    static Stream<Arguments> sources() {
        return Stream.of(
                arguments("JPEG", jpeg(300, 200)),
                arguments("PNG", png(300, 200)),
                arguments("WebP", webp(300, 200)));
    }

    static Stream<Arguments> unreadableSources() {
        byte[] png = png(200, 200);
        byte[] webp = webp(200, 200);
        return Stream.of(
                arguments("an empty file", new byte[0]),
                arguments("random bytes", randomBytes(400, 7)),
                arguments("a JPEG header followed by garbage",
                        concat(bytes(0xFF, 0xD8, 0xFF, 0xE0), randomBytes(400, 7))),
                arguments("a truncated PNG", Arrays.copyOf(png, png.length * 6 / 10)),
                arguments("a truncated WebP", Arrays.copyOf(webp, webp.length / 2)),
                arguments("a PNG that claims zero width", pngHeaderOnly(0, 100)),
                arguments("a PNG that claims zero height", pngHeaderOnly(100, 0)));
    }

    @Nested
    class HappyPaths {

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.nemouw.media.processing.ImageProcessorTest#sources")
        void producesAWebpFromEverySupportedFormat(String format, byte[] source) {
            assertSize(processor().toWebp(source, fit(1000, 1000, 80)), 300, 200);
        }

        @ParameterizedTest
        @CsvSource({"200,100", "100,200", "20,10", "64,64"})
        void cropsEverySourceToTheExactTargetSizeAndUpscalesSmallOnes(int width, int height) {
            assertSize(processor().toWebp(png(width, height), crop(64, 64, 80)), 64, 64);
        }

        @Test
        void cropsToANonSquareTarget() {
            assertSize(processor().toWebp(png(400, 400), crop(160, 60, 80)), 160, 60);
        }

        @ParameterizedTest
        @CsvSource({
                "300, 150, 100, 50",
                "150, 300, 50,  100",
                "300, 300, 100, 100",
                "80,  60,  80,  60",
                "100, 100, 100, 100"
        })
        void fitsInsideTheLimitKeepingTheAspectRatioAndNeverUpscaling(int width, int height, int expectedWidth,
                                                                      int expectedHeight) {
            assertSize(processor().toWebp(png(width, height), fit(100, 100, 80)), expectedWidth, expectedHeight);
        }

        @Test
        void keepsTheAspectRatioWhenOnlyOnePixelOverTheLimit() {
            BufferedImage image = webpOf(processor().toWebp(png(101, 100), fit(100, 100, 80)));

            assertThat(image.getWidth()).isEqualTo(100);
            assertThat(image.getHeight()).isBetween(99, 100);
        }

        @Test
        void givesALargerFileAtHigherQuality() {
            byte[] source = noiseJpeg(200, 200);

            int high = processor().toWebp(source, fit(200, 200, 90)).length;
            int low = processor().toWebp(source, fit(200, 200, 10)).length;

            assertThat(high).isGreaterThan(low);
        }

        @Test
        void keepsTransparencyInsteadOfTurningItBlackOrOpaque() {
            BufferedImage image = webpOf(processor().toWebp(transparentPng(40, 40), fit(40, 40, 90)));

            int transparent = image.getRGB(5, 20);
            int opaque = image.getRGB(35, 20);
            assertThat(transparent >>> 24).isLessThanOrEqualTo(10);
            assertThat(opaque >>> 24).isGreaterThanOrEqualTo(245);
            assertThat((opaque >> 16) & 0xFF).isGreaterThan(180);
            assertThat((opaque >> 8) & 0xFF).isLessThan(80);
            assertThat(opaque & 0xFF).isLessThan(80);
        }

        @Test
        void dropsExifAndGpsMetadataFromTheOutput() {
            byte[] source = jpegWithExif(300, 100, 1, SECRET);
            assertThat(contains(source, "Exif\0\0")).isTrue();
            assertThat(contains(source, SECRET)).isTrue();

            byte[] output = processor().toWebp(source, fit(1000, 1000, 80));

            assertThat(contains(output, SECRET)).isFalse();
            assertThat(contains(output, "EXIF")).isFalse();
            assertThat(contains(output, "XMP ")).isFalse();
        }

        @Test
        void appliesTheExifOrientationBeforeResizing() {
            byte[] rotated = jpegWithExif(300, 100, 6, "x");
            byte[] upright = jpegWithExif(300, 100, 1, "x");
            assertThat(exifOrientation(rotated)).isEqualTo(6);
            assertThat(exifOrientation(upright)).isEqualTo(1);

            assertSize(processor().toWebp(rotated, fit(1000, 1000, 80)), 100, 300);
            assertSize(processor().toWebp(upright, fit(1000, 1000, 80)), 300, 100);
        }
    }

    @Nested
    class BoundaryValues {

        @Test
        void acceptsExactlyTheMaximumPixelsAndRejectsOneMore() {
            ImageProcessor processor = processor(10_000);

            assertSize(processor.toWebp(png(100, 100), fit(1000, 1000, 80)), 100, 100);
            assertThat(errorsOf(() -> processor.toWebp(png(101, 100), fit(1000, 1000, 80))))
                    .containsExactly(TOO_LARGE);
        }

        @Test
        void checksTheLimitFromTheHeaderAloneSoNoPixelsNeedToExist() {
            ImageProcessor processor = processor();

            assertThat(errorsOf(() -> processor.toWebp(pngHeaderOnly(5_000, 5_000), fit(100, 100, 80))))
                    .containsExactly(UNREADABLE);
            assertThat(errorsOf(() -> processor.toWebp(pngHeaderOnly(5_001, 5_000), fit(100, 100, 80))))
                    .containsExactly(TOO_LARGE);
        }

        @ParameterizedTest
        @CsvSource({"65536,65536", "70000,70000", "100000,100000"})
        void doesNotOverflowOnHugeDimensions(int width, int height) {
            assertThat(errorsOf(() -> processor().toWebp(pngHeaderOnly(width, height), fit(100, 100, 80))))
                    .containsExactly(TOO_LARGE);
        }

        @Test
        void rejectsAVeryWideImageOnePixelOverTheLimit() {
            byte[] wide = encode(new BufferedImage(100_000, 1, BufferedImage.TYPE_INT_RGB), "png");

            assertThat(errorsOf(() -> processor(99_999).toWebp(wide, fit(64, 64, 80)))).containsExactly(TOO_LARGE);
        }

        @Test
        void handlesAVeryWideImageWhenCroppingItToASquare() {
            byte[] wide = encode(new BufferedImage(100_000, 1, BufferedImage.TYPE_INT_RGB), "png");

            assertSize(processor(100_000).toWebp(wide, crop(64, 64, 80)), 64, 64);
        }

        @Test
        void handlesAVeryWideImageWhenFittingItInsideASquare() {
            byte[] wide = encode(new BufferedImage(100_000, 1, BufferedImage.TYPE_INT_RGB), "png");

            BufferedImage image = webpOf(processor(100_000).toWebp(wide, fit(64, 64, 80)));

            assertThat(image.getWidth()).isLessThanOrEqualTo(64);
            assertThat(image.getHeight()).isGreaterThanOrEqualTo(1);
        }
    }

    @Nested
    class ErrorPaths {

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.nemouw.media.processing.ImageProcessorTest#unreadableSources")
        void rejectsAFileThatIsNotAReadableImage(String label, byte[] source) {
            assertThat(errorsOf(() -> processor().toWebp(source, fit(100, 100, 80)))).containsExactly(UNREADABLE);
        }
    }

    @Nested
    class DependencyFailures {

        @Test
        void answersBusyWhenTheJobOutlastsTheTimeout() {
            ImageProcessor processor = processor(1, 1, Duration.ZERO, MAX_PIXELS);
            byte[] heavy = noiseJpeg(1600, 1600);

            assertBusy(() -> processor.toWebp(heavy, fit(800, 800, 80)));
        }

        @Test
        void answersBusyAndKeepsTheInterruptFlagWhenTheCallerIsInterrupted() {
            ImageProcessor processor = processor();
            byte[] heavy = noiseJpeg(1600, 1600);

            Thread.currentThread().interrupt();
            Throwable thrown = catchThrowable(() -> processor.toWebp(heavy, fit(800, 800, 80)));
            boolean stillInterrupted = Thread.interrupted();

            assertThat(stillInterrupted).isTrue();
            assertBusy(thrown);
        }

        @Test
        void answersBusyAfterShutdown() {
            ImageProcessor processor = processor();
            processor.shutdown();

            assertBusy(() -> processor.toWebp(png(10, 10), fit(10, 10, 80)));
        }

        @Test
        void answersBusyWhenEveryThreadAndQueueSlotIsTaken() throws Exception {
            ThreadPoolExecutor pool = pool(1, 1);
            ImageProcessor processor = track(
                    new ImageProcessor(new Processing(1, 1, Duration.ofSeconds(5), MAX_PIXELS), pool));
            CountDownLatch running = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            pool.submit(() -> {
                running.countDown();
                release.await();
                return null;
            });
            assertThat(running.await(5, TimeUnit.SECONDS)).isTrue();
            pool.submit(() -> null);

            try {
                assertBusy(() -> processor.toWebp(png(10, 10), fit(10, 10, 80)));
            } finally {
                release.countDown();
            }
        }
    }
}