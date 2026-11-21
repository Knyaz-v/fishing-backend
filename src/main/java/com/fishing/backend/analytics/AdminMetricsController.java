package com.fishing.backend.analytics;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/metrics")
@RequiredArgsConstructor
@Tag(name = "Метрики (админ)", description = "Агрегаты под пороги успеха")
@SecurityRequirement(name = "bearerAuth")
public class AdminMetricsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/summary")
    @Operation(summary = "Сводка метрик за последние 30 дней")
    public Map<String, Object> getSummary() {
        return analyticsService.getSummary();
    }
}