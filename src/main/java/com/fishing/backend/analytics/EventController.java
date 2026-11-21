package com.fishing.backend.analytics;

import com.fishing.backend.analytics.dto.EventRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Tag(name = "Аналитика", description = "Запись событий аналитики")
public class EventController {

    private final AnalyticsService analyticsService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Записать событие аналитики")
    public void record(@Valid @RequestBody EventRequest request) {
        analyticsService.record(request);
    }
}