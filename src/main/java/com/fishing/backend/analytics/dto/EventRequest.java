package com.fishing.backend.analytics.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record EventRequest(
        @NotBlank
        String eventType,

        UUID userId,
        String sessionId,
        Long waterBodyId,
        String properties
) { }