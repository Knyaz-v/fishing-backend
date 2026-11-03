package com.fishing.backend.photo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class LocalStorageService implements StorageService {

    private final Path storagePath;

    // Берем путь из настроек (в .env: STORAGE_PATH)
    public LocalStorageService(@Value("${app.storage.path:./uploads}") String storagePath) {
        this.storagePath = Paths.get(storagePath);
    }

    @Override
    public String store(byte[] fileContent, String originalFileName) {
        try {
            // 1. Генерируем уникальное имя файла
            String extension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                extension = originalFileName.substring(originalFileName.lastIndexOf('.'));
            }

            String key = UUID.randomUUID().toString() + extension;

            // 2. Создаем папку, если её нет
            Files.createDirectories(storagePath);

            // 3. Сохраняем файл
            Path targetPath = storagePath.resolve(key);
            Files.write(targetPath, fileContent);

            return key;

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    @Override
    public byte[] load(String key) {
        try {
            Path filePath = storagePath.resolve(key);
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load file", e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Path filePath = storagePath.resolve(key);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file", e);
        }
    }
}
