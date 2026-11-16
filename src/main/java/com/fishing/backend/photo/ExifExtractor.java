package com.fishing.backend.photo;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.jpeg.JpegDirectory;
import com.drew.metadata.png.PngDirectory;
import com.drew.metadata.webp.WebpDirectory;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;


@Component
public class ExifExtractor {

    private final GeometryFactory geometryFactory = new GeometryFactory();

    public ExifData extract(byte[] fileContent) {
        try {
            Metadata metadata = ImageMetadataReader
                    .readMetadata(new ByteArrayInputStream(fileContent));

            // 1. Читаем дату
            LocalDateTime dateTime = null;
            ExifSubIFDDirectory exifDir = metadata
                    .getFirstDirectoryOfType(ExifSubIFDDirectory.class);

            if (exifDir != null) {
                Date date = exifDir.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
                if (date != null) {
                    dateTime = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
                }
            }

            // 2. Читаем GPS
            Point location = null;
            GpsDirectory gpsDir = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            if (gpsDir != null && gpsDir.getGeoLocation() != null) {
                double lat = gpsDir.getGeoLocation().getLatitude();
                double lon = gpsDir.getGeoLocation().getLongitude();
                location = geometryFactory.createPoint(new Coordinate(lon, lat));
            }

            // 3. Читаем ширину и высоту
            Integer width = null;
            Integer height = null;

            JpegDirectory jpegDir = metadata.getFirstDirectoryOfType(JpegDirectory.class);
            if (jpegDir != null) {
                width = jpegDir.getImageWidth();
                height = jpegDir.getImageHeight();
            } else {
                PngDirectory pngDir = metadata.getFirstDirectoryOfType(PngDirectory.class);
                if (pngDir != null) {
                    width = pngDir.getInt(PngDirectory.TAG_IMAGE_WIDTH);
                    height = pngDir.getInt(PngDirectory.TAG_IMAGE_HEIGHT);
                } else {
                    WebpDirectory webpDir = metadata.getFirstDirectoryOfType(WebpDirectory.class);
                    if (webpDir != null) {
                        width = webpDir.getInt(WebpDirectory.TAG_IMAGE_WIDTH);
                        height = webpDir.getInt(WebpDirectory.TAG_IMAGE_HEIGHT);
                    }
                }
            }

            return new ExifData(dateTime, location, width, height);

        } catch (Exception e) {
            // Если EXIF нет или файл поврежден, просто возвращаем пустые данные
            return new ExifData(null, null, null, null);
        }
    }

    // Простой record для передачи данных
    public record ExifData(
            LocalDateTime dateTime,
            Point location,
            Integer width,
            Integer height
    ) {}
}