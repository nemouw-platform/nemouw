package com.nemouw.media.processing;

import com.nemouw.common.exception.ApiException;
import com.nemouw.common.exception.Errors;
import com.nemouw.media.MediaProperties;
import com.nemouw.media.MediaProperties.Limits;
import jakarta.annotation.PreDestroy;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Re-encodes images to WebP on a dedicated bounded pool.
 */
@Component
public class ImageProcessor {
    private static final int RETRY_AFTER_SECONDS = 3;

    private record Size(int width, int height) {
    }

    private final ThreadPoolExecutor pool;
    private final Duration timeout;
    private final long maxPixels;
    private final WebpEncoder encoder = new WebpEncoder();

    ImageProcessor(MediaProperties props) {
        MediaProperties.Processing p = props.processing();
        this.timeout = p.timeout();
        this.maxPixels = p.maxPixels();
        this.pool = new ThreadPoolExecutor(p.threads(), p.threads(), 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(p.queueCapacity()), namedThreads(), new ThreadPoolExecutor.AbortPolicy());
    }

    public byte[] toWebp(byte[] source, Limits limits) {
        Future<byte[]> job;
        try {
            job = pool.submit(() -> render(source, limits));
        } catch (RejectedExecutionException e) {
            throw Errors.mediaBusy(RETRY_AFTER_SECONDS);
        }
        try {
            return job.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            job.cancel(true);
            throw Errors.mediaBusy(RETRY_AFTER_SECONDS);
        } catch (InterruptedException e) {
            job.cancel(true);
            Thread.currentThread().interrupt();
            throw Errors.mediaBusy(RETRY_AFTER_SECONDS);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ApiException api) throw api;
            if (cause instanceof IOException) throw unreadable();
            throw new IllegalStateException("Image processing failed", cause);
        }
    }

    @PreDestroy
    void shutdown() {
        pool.shutdownNow();
    }

    private byte[] render(byte[] source, Limits limits) throws IOException {
        Size size = sizeOf(source);
        if ((long) size.width() * size.height() > maxPixels) {
            throw Errors.invalidField("file", "the image dimensions are too large");
        }
        return encoder.encode(resized(source, size, limits), limits.quality() / 100f);
    }

    /** Reads the header only, before any pixel is decoded. */
    private static Size sizeOf(byte[] source) throws IOException {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(source))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) throw new IOException("No image reader");
            ImageReader reader = readers.next();
            try {
                reader.setInput(in);
                return new Size(reader.getWidth(0), reader.getHeight(0));
            } finally {
                reader.dispose();
            }
        }
    }

    /** Applies EXIF orientation, then crops to fill or fits inside the target without upscaling. */
    private static BufferedImage resized(byte[] source, Size size, Limits limits) throws IOException {
        var builder = Thumbnails.of(new ByteArrayInputStream(source)).imageType(BufferedImage.TYPE_INT_ARGB);
        if (limits.crop()) {
            builder.size(limits.width(), limits.height()).crop(Positions.CENTER);
        } else if (size.width() > limits.width() || size.height() > limits.height()) {
            builder.size(limits.width(), limits.height());
        } else {
            builder.scale(1.0);
        }
        return builder.asBufferedImage();
    }

    private static ApiException unreadable() {
        return Errors.invalidField("file", "the file could not be read");
    }

    private static ThreadFactory namedThreads() {
        AtomicInteger count = new AtomicInteger();
        return task -> {
            Thread thread = new Thread(task, "media-image-" + count.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }
}