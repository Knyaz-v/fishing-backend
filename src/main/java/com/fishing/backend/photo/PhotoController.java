package com.fishing.backend.photo;

import com.fishing.backend.photo.dto.PhotoDownload;
import com.fishing.backend.photo.dto.PhotoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/catches/{catchId}/photos")
@RequiredArgsConstructor
@Tag(name = "Фото уловов", description = "Загрузка фотографий к улову")
@SecurityRequirement(name = "bearerAuth")
public class PhotoController {

    private final PhotoService photoService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Загрузить фото к улову", description = "Загружает фото, читает EXIF (дата, GPS), сохраняет файл на диск.")
    @ApiResponse(responseCode = "201", description = "Фото загружено")
    @ApiResponse(responseCode = "400", description = "Файл пустой, слишком большой или неподдерживаемый тип")
    @ApiResponse(responseCode = "403", description = "Нет прав на изменение этого улова")
    @ApiResponse(responseCode = "404", description = "Улов не найден")
    public PhotoResponse upload(
            @Parameter(description = "UUID улова", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID catchId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(Objects.requireNonNull(jwt.getSubject()));
        return photoService.upload(catchId, file, userId);
    }

    @GetMapping("/{photoId}")
    @Operation(summary = "Скачать фото по ID", description = "Возвращает файл фото. Владельцу — оригинал, остальным — без EXIF-GPS (для JPEG).")
    @ApiResponse(responseCode = "200", description = "Файл фото")
    @ApiResponse(responseCode = "403", description = "Нет прав на просмотр")
    @ApiResponse(responseCode = "404", description = "Фото или улов не найдены")
    public ResponseEntity<byte[]> download(
            @Parameter(description = "UUID улова", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID catchId,
            @Parameter(description = "UUID фото", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID photoId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(Objects.requireNonNull(jwt.getSubject()));
        PhotoDownload result = photoService.download(catchId, photoId, userId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.contentType()))
                .body(result.bytes());
    }

    @GetMapping
    @Operation(summary = "Получить список фото улова", description = "Возвращает список метаданных фото (без координат).")
    @ApiResponse(responseCode = "200", description = "Список фото")
    @ApiResponse(responseCode = "403", description = "Нет прав на просмотр")
    @ApiResponse(responseCode = "404", description = "Улов не найден")
    public List<PhotoResponse> getPhotos(
            @Parameter(description = "UUID улова", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID catchId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(Objects.requireNonNull(jwt.getSubject()));
        return photoService.getPhotosByCatchId(catchId, userId);
    }

    @DeleteMapping("/{photoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить фото улова", description = "Удаляет файл с диска и запись из БД.")
    @ApiResponse(responseCode = "204", description = "Фото удалено")
    @ApiResponse(responseCode = "403", description = "Нет прав на удаление")
    @ApiResponse(responseCode = "404", description = "Фото или улов не найдены")
    public void delete(
            @Parameter(description = "UUID улова", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID catchId,
            @Parameter(description = "UUID фото", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID photoId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(Objects.requireNonNull(jwt.getSubject()));
        photoService.delete(catchId, photoId, userId);
    }
}