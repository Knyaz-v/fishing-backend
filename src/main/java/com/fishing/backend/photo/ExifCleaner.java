package com.fishing.backend.photo;

import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Component
public class ExifCleaner {

    /**
     * Удаляет EXIF (включая GPS) из JPEG-файла без потери качества.
     * Для PNG перезаписываем файл, тем самым очищая EXIF (включая GPS).
     * Для WebP возвращает оригинал (нет бесплатного решения).
     */
    public byte[] removeExif(byte[] originalBytes, String contentType) {
        if (contentType == null) return originalBytes;

        // JPEG: вырезаем EXIF-сегмент без потери качества
        if (contentType.contains("jpeg") || contentType.contains("jpg")) {
            try {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                new ExifRewriter().removeExifMetadata(
                        new ByteArrayInputStream(originalBytes),
                        outputStream
                );
                return outputStream.toByteArray();
            } catch (Exception e) {
                return originalBytes;
            }
        }

        // PNG: пересохраняем через ImageIO (качество не теряется, PNG без потерь)
        if (contentType.contains("png")) {
            try {
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(originalBytes));
                if (image == null) return originalBytes;

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ImageIO.write(image, "png", outputStream);
                return outputStream.toByteArray();
            } catch (Exception e) {
                return originalBytes;
            }
        }

        // WebP: пока возвращаем оригинал (нет бесплатного решения)
        return originalBytes;
    }
}