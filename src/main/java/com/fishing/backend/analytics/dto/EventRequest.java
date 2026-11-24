package com.fishing.backend.analytics.dto;

import com.fishing.backend.analytics.EventType;
import jakarta.validation.constraints.NotNull;

public record EventRequest(
        @NotNull
        EventType eventType,

        String sessionId,
        Long waterBodyId,
        String properties
) { }