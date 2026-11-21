package com.fishing.backend.analytics;

import com.fishing.backend.analytics.dto.EventRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final EventRepository eventRepo;

    @Transactional
    public void record(EventRequest request) {
        EventEntity event = new EventEntity();
        event.setEventType(request.eventType());
        event.setUserId(request.userId());
        event.setSessionId(request.sessionId());
        event.setWaterBodyId(request.waterBodyId());
        event.setProperties(request.properties() != null ? request.properties() : "{}");
        eventRepo.save(event);
    }

    // Агрегаты под пороги успеха
    @Transactional(readOnly = true)
    public Map<String, Object> getSummary() {
        LocalDateTime from = LocalDateTime.now().minusDays(30); // за последние 30 дней

        Map<String, Object> summary = new HashMap<>();
        summary.put("map_opened", eventRepo
                .countByEventTypeSince("map_opened", from));
        summary.put("water_body_viewed", eventRepo
                .countByEventTypeSince("water_body_viewed", from));
        summary.put("catch_created", eventRepo
                .countByEventTypeSince("catch_created", from));
        summary.put("catch_history_viewed", eventRepo
                .countByEventTypeSince("catch_history_viewed", from));

        // Уникальные пользователи (если нужны)
        summary.put("unique_users_map_opened", eventRepo
                .countDistinctUsersByEventTypeSince("map_opened", from));
        summary.put("unique_users_catch_created", eventRepo
                .countDistinctUsersByEventTypeSince("catch_created", from));

        return summary;
    }
}