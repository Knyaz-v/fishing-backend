package com.fishing.backend.photo;

public interface StorageService {
    /**
     * Сохраняет файл и возвращает уникальный ключ, по которому его можно найти.
     */
    String store(byte[] fileContent, String originalFileName);

    // Выгружает файл
    byte[] load(String key);

    void delete(String key);
}
