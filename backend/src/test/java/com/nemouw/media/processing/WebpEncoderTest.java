package com.nemouw.media.processing;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

import static com.nemouw.media.processing.ImageFixtures.decode;
import static com.nemouw.media.processing.ImageFixtures.gradient;
import static com.nemouw.media.processing.ImageFixtures.noise;
import static org.assertj.core.api.Assertions.assertThat;

class WebpEncoderTest {

    private final WebpEncoder encoder = new WebpEncoder();

    @Nested
    class HappyPaths {

        @Test
        void hasAWebpWriterAndReaderRegisteredOnThisPlatform() {
            assertThat(ImageIO.getImageWritersByMIMEType("image/webp").hasNext()).isTrue();
            assertThat(ImageIO.getImageReadersByMIMEType("image/webp").hasNext()).isTrue();
        }

        @Test
        void encodesAWebpThatDecodesToTheSameSize() throws IOException {
            byte[] webp = encoder.encode(gradient(64, 48), 0.8f);

            BufferedImage decoded = decode(webp);
            assertThat(FileKind.detect(webp)).contains(FileKind.WEBP);
            assertThat(decoded.getWidth()).isEqualTo(64);
            assertThat(decoded.getHeight()).isEqualTo(48);
        }

        @Test
        void encodesAnImageWithAnAlphaChannel() throws IOException {
            BufferedImage transparent = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);

            BufferedImage decoded = decode(encoder.encode(transparent, 0.8f));

            assertThat(decoded.getWidth()).isEqualTo(32);
            assertThat(decoded.getHeight()).isEqualTo(32);
        }
    }

    @Nested
    class BoundaryValues {

        @Test
        void givesALargerFileAtHigherQuality() throws IOException {
            BufferedImage image = noise(128, 128, 3);

            assertThat(encoder.encode(image, 0.9f).length).isGreaterThan(encoder.encode(image, 0.1f).length);
        }

        @ParameterizedTest
        @ValueSource(floats = {0.01f, 1.0f})
        void decodesAtTheExtremeQualities(float quality) throws IOException {
            BufferedImage decoded = decode(encoder.encode(gradient(32, 32), quality));

            assertThat(decoded.getWidth()).isEqualTo(32);
        }
    }
}