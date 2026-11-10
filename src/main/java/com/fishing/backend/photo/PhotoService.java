package com.fishing.backend.photo;

import com.fishing.backend.catches.CatchEntity;
import com.fishing.backend.catches.CatchRepository;
import com.fishing.backend.photo.dto.PhotoDownload;
import com.fishing.backend.photo.dto.PhotoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PhotoService {

    private final StorageService storageService;
    private final CatchPhotoRepository catchPhotoRepo;
    private final CatchRepository catchRepo;
    private final ExifExtractor exifExtractor;
    private final ExifCleaner exifCleaner;

    // Максимальный размер файла (5 МБ)
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    // Разрешенные типы
    private static final List<String> ALLOWED_TYPES = List.of(
            "image/jpeg", "image/png", "image/webp");

    @Transactional
    public PhotoResponse upload(UUID catchId, MultipartFile file, boolean updateCaughtAtFromExif, UUID userId) {

        // 1. Валидация
        validateFile(file);

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to read file bytes", e);
        }

        // 2. Извлечение EXIF
        ExifExtractor.ExifData exifData = exifExtractor.extract(bytes);

        // 3. Находим улов и проверяем владельца
        CatchEntity catchEntity = catchRepo.findById(catchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Catch not found"));

        if (!catchEntity.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You don't have permission to add photos to this catch");
        }

        // 4. Обновляем улов данными из EXIF
        if (updateCaughtAtFromExif && exifData.dateTime() != null) {
            catchEntity.setCaughtAt(exifData.dateTime());
        }
        if (exifData.location() != null) {
            catchEntity.setLocation(exifData.location());
        }

        // 5. Сохраняем улов в БД
        catchRepo.save(catchEntity);

        // 6. Считаем sortOrder
        List<CatchPhotoEntity> existingPhotos = catchPhotoRepo
                .findByCatchIdOrderBySortOrderAsc(catchId);
        short nextSortOrder = (short) existingPhotos.size();

        // 7. Создаем сущность фото и сохраняем её в БД
        CatchPhotoEntity entity = new CatchPhotoEntity();
        entity.setCatchId(catchId);
        entity.setContentType(file.getContentType());
        entity.setSizeBytes(file.getSize());
        entity.setWidth(exifData.width());
        entity.setHeight(exifData.height());
        entity.setSortOrder(nextSortOrder);

        // 8. Сохраняем файл на диск
        String storageKey = storageService.store(bytes, file.getOriginalFilename());
        entity.setStorageKey(storageKey);

        // 9. Сохраняем сущность фото в БД
        CatchPhotoEntity saved = catchPhotoRepo.save(entity);

        return new PhotoResponse(
                saved.getId(),
                saved.getStorageKey(),
                saved.getContentType(),
                saved.getSizeBytes(),
                saved.getWidth(),
                saved.getHeight()
        );
    }

    @Transactional(readOnly = true)
    public PhotoDownload download(UUID catchId, UUID photoId, UUID userId) {
        CatchPhotoEntity photo = catchPhotoRepo.findById(photoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Photo not found"));

        if (!photo.getCatchId().equals(catchId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Photo does not belong to this catch");
        }

        CatchEntity catchEntity = catchRepo.findById(catchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Catch not found"));

        boolean isOwner = catchEntity.getUserId().equals(userId);
        boolean isPublic = "PUBLIC".equals(catchEntity.getVisibility());

        if (!isOwner && !isPublic) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        byte[] bytes = storageService.load(photo.getStorageKey());

        // Если это не владелец и улов публичный — удаляем EXIF-GPS
        if (!isOwner) {
            bytes = exifCleaner.removeExif(bytes, photo.getContentType());
        }

        return new PhotoDownload(bytes, photo.getContentType());
    }

    @Transactional(readOnly = true)
    public List<PhotoResponse> getPhotosByCatchId(UUID catchId, UUID userId) {
        // 1. Проверяем, что улов существует
        CatchEntity catchEntity = catchRepo.findById(catchId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Catch not found"));

        // 2. Проверяем доступ (владелец или публичный улов)
        boolean isOwner = catchEntity.getUserId().equals(userId);
        boolean isPublic = "PUBLIC".equals(catchEntity.getVisibility());

        if (!isOwner && !isPublic) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        // 3. Возвращаем список фото в порядке sortOrder
        return catchPhotoRepo.findByCatchIdOrderBySortOrderAsc(catchId)
                .stream()
                .map(entity -> new PhotoResponse(
                        entity.getId(),
                        entity.getStorageKey(),
                        entity.getContentType(),
                        entity.getSizeBytes(),
                        entity.getWidth(),
                        entity.getHeight()
                ))
                .toList();
    }

    @Transactional
    public void delete(UUID catchId, UUID photoId, UUID userId) {
        // 1. Находим фото
        CatchPhotoEntity photo = catchPhotoRepo.findById(photoId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Photo not found"));

        // 2. Проверяем, что фото принадлежит этому улову
        if (!photo.getCatchId().equals(catchId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Photo does not belong to this catch");
        }

        // 3. Проверяем владельца улова
        CatchEntity catchEntity = catchRepo.findById(catchId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Catch not found"));

        if (!catchEntity.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        // 4. Удаляем файл с диска
        storageService.delete(photo.getStorageKey());

        // 5. Удаляем запись из БД
        catchPhotoRepo.delete(photo);
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File size exceeds 5 MB");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported file type. Allowed: JPEG, PNG, WEBP");
        }
    }
}