package com.fishing.backend.photo.dto;

import java.util.UUID;

public record PhotoResponse(
        UUID id,
        String storageKey,
        String contentType,
        Long sizeBytes,
        Integer width,
        Integer height
) {}
