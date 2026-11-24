package com.fishing.backend.analytics;

import com.fishing.backend.analytics.dto.EventRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final EventRepository eventRepo;

    @Transactional
    public void record(EventRequest request, UUID userId) {
        EventEntity event = new EventEntity();
        event.setEventType(request.eventType());
        event.setUserId(userId);
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
                .countByEventTypeSince(EventType.MAP_OPENED, from));
        summary.put("water_body_viewed", eventRepo
                .countByEventTypeSince(EventType.WATER_BODY_VIEWED, from));
        summary.put("catch_created", eventRepo
                .countByEventTypeSince(EventType.CATCH_CREATED, from));
        summary.put("catch_history_viewed", eventRepo
                .countByEventTypeSince(EventType.CATCH_HISTORY_VIEWED, from));

        // Уникальные пользователи (если нужны)
        summary.put("unique_users_map_opened", eventRepo
                .countDistinctUsersByEventTypeSince(EventType.MAP_OPENED, from));
        summary.put("unique_users_catch_created", eventRepo
                .countDistinctUsersByEventTypeSince(EventType.CATCH_CREATED, from));

        return summary;
    }
}