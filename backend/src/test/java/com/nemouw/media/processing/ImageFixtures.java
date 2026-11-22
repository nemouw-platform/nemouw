package com.nemouw.media.processing;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.CRC32;

/** Images and broken images built in memory, so no binary files live in the repository. */
final class ImageFixtures {

    private static final int EXIF_ORIENTATION_TAG = 0x0112;

    private ImageFixtures() {
    }

    static byte[] bytes(int... values) {
        byte[] result = new byte[values.length];
        for (int i = 0; i < values.length; i++) result[i] = (byte) values[i];
        return result;
    }

    static byte[] ascii(String text) {
        return text.getBytes(StandardCharsets.ISO_8859_1);
    }

    static byte[] concat(byte[]... parts) {
        int length = 0;
        for (byte[] part : parts) length += part.length;
        byte[] result = new byte[length];
        int at = 0;
        for (byte[] part : parts) {
            System.arraycopy(part, 0, result, at, part.length);
            at += part.length;
        }
        return result;
    }

    static byte[] randomBytes(int length, long seed) {
        byte[] result = new byte[length];
        new Random(seed).nextBytes(result);
        return result;
    }

    static boolean contains(byte[] haystack, String needle) {
        return new String(haystack, StandardCharsets.ISO_8859_1).contains(needle);
    }

    static BufferedImage gradient(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int red = x * 255 / Math.max(1, width - 1);
                int green = y * 255 / Math.max(1, height - 1);
                image.setRGB(x, y, (red << 16) | (green << 8) | 128);
            }
        }
        return image;
    }

    static BufferedImage noise(int width, int height, long seed) {
        Random random = new Random(seed);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) image.setRGB(x, y, random.nextInt() & 0xFFFFFF);
        }
        return image;
    }

    static byte[] encode(BufferedImage image, String format) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, format, out)) throw new IllegalStateException("No writer for " + format);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static byte[] png(int width, int height) {
        return encode(gradient(width, height), "png");
    }

    static byte[] jpeg(int width, int height) {
        return encode(gradient(width, height), "jpg");
    }

    static byte[] noiseJpeg(int width, int height) {
        return encode(noise(width, height, 1), "jpg");
    }

    static byte[] webp(int width, int height) {
        try {
            return new WebpEncoder().encode(gradient(width, height), 0.8f);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The left half is fully transparent, the right half opaque red. */
    static byte[] transparentPng(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) image.setRGB(x, y, x < width / 2 ? 0x00000000 : 0xFFFF0000);
        }
        return encode(image, "png");
    }

    static BufferedImage decode(byte[] image) {
        try {
            BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(image));
            if (decoded == null) throw new IllegalStateException("No reader for this image");
            return decoded;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** A real JPEG with an EXIF segment holding the orientation and a text tag, injected after the JFIF header. */
    static byte[] jpegWithExif(int width, int height, int orientation, String text) {
        byte[] jpeg = encode(gradient(width, height), "jpg");
        byte[] textBytes = (text + "\0").getBytes(StandardCharsets.US_ASCII);
        int textOffset = 38;
        ByteArrayOutputStream tiffBytes = new ByteArrayOutputStream();
        DataOutputStream tiff = new DataOutputStream(tiffBytes);
        try {
            tiff.write(new byte[]{'I', 'I', 0x2A, 0x00});
            littleEndian32(tiff, 8);
            littleEndian16(tiff, 2);
            littleEndian16(tiff, 0x010E);
            littleEndian16(tiff, 2);
            littleEndian32(tiff, textBytes.length);
            littleEndian32(tiff, textOffset);
            littleEndian16(tiff, EXIF_ORIENTATION_TAG);
            littleEndian16(tiff, 3);
            littleEndian32(tiff, 1);
            littleEndian16(tiff, orientation);
            littleEndian16(tiff, 0);
            littleEndian32(tiff, 0);
            tiff.write(textBytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        byte[] payload = concat(ascii("Exif\0\0"), tiffBytes.toByteArray());
        int segmentLength = payload.length + 2;
        byte[] segment = concat(bytes(0xFF, 0xE1, segmentLength >> 8, segmentLength & 0xFF), payload);
        int insertAt = 2;
        if ((jpeg[2] & 0xFF) == 0xFF && (jpeg[3] & 0xFF) == 0xE0) {
            insertAt = 4 + (((jpeg[4] & 0xFF) << 8) | (jpeg[5] & 0xFF));
        }
        return concat(Arrays.copyOfRange(jpeg, 0, insertAt), segment, Arrays.copyOfRange(jpeg, insertAt, jpeg.length));
    }

    /** Reads the orientation the way image libraries do, through ImageIO's metadata; -1 when there is none. */
    static int exifOrientation(byte[] jpeg) {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(jpeg))) {
            ImageReader reader = ImageIO.getImageReaders(in).next();
            try {
                reader.setInput(in);
                IIOMetadataNode root = (IIOMetadataNode) reader.getImageMetadata(0)
                        .getAsTree("javax_imageio_jpeg_image_1.0");
                IIOMetadataNode sequence = (IIOMetadataNode) root.getElementsByTagName("markerSequence").item(0);
                for (var node = sequence.getFirstChild(); node != null; node = node.getNextSibling()) {
                    IIOMetadataNode marker = (IIOMetadataNode) node;
                    if (!"unknown".equals(marker.getNodeName()) || !"225".equals(marker.getAttribute("MarkerTag"))) {
                        continue;
                    }
                    byte[] data = (byte[]) marker.getUserObject();
                    if (data.length < 14 || !new String(data, 0, 4, StandardCharsets.ISO_8859_1).equals("Exif")) {
                        continue;
                    }
                    boolean little = data[6] == 'I';
                    int firstEntries = 6 + int32(data, 10, little);
                    int entries = int16(data, firstEntries, little);
                    for (int i = 0; i < entries; i++) {
                        int at = firstEntries + 2 + i * 12;
                        if (int16(data, at, little) == EXIF_ORIENTATION_TAG) return int16(data, at + 8, little);
                    }
                }
                return -1;
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Signature, IHDR with the given size and an empty IDAT: enough to report dimensions, no pixels at all. */
    static byte[] pngHeaderOnly(int width, int height) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream(); DataOutputStream out = new DataOutputStream(bytes)) {
            out.write(bytes(0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A));
            ByteArrayOutputStream header = new ByteArrayOutputStream();
            DataOutputStream headerOut = new DataOutputStream(header);
            headerOut.writeInt(width);
            headerOut.writeInt(height);
            headerOut.write(new byte[]{8, 2, 0, 0, 0});
            chunk(out, "IHDR", header.toByteArray());
            chunk(out, "IDAT", new byte[0]);
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void chunk(DataOutputStream out, String type, byte[] data) throws IOException {
        byte[] typeBytes = type.getBytes(StandardCharsets.US_ASCII);
        out.writeInt(data.length);
        out.write(typeBytes);
        out.write(data);
        CRC32 crc = new CRC32();
        crc.update(typeBytes);
        crc.update(data);
        out.writeInt((int) crc.getValue());
    }

    private static void littleEndian16(DataOutputStream out, int value) throws IOException {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
    }

    private static void littleEndian32(DataOutputStream out, int value) throws IOException {
        littleEndian16(out, value & 0xFFFF);
        littleEndian16(out, (value >>> 16) & 0xFFFF);
    }

    private static int int16(byte[] data, int at, boolean little) {
        return little ? (data[at] & 0xFF) | ((data[at + 1] & 0xFF) << 8)
                : ((data[at] & 0xFF) << 8) | (data[at + 1] & 0xFF);
    }

    private static int int32(byte[] data, int at, boolean little) {
        return little ? int16(data, at, true) | (int16(data, at + 2, true) << 16)
                : (int16(data, at, false) << 16) | int16(data, at + 2, false);
    }
}