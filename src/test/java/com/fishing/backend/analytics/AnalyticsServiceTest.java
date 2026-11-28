package com.fishing.backend.analytics;

import com.fishing.backend.TestcontainersConfiguration;
import com.fishing.backend.analytics.dto.EventRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// Проверяет запись событий и агрегаты под пороги успеха (счётчики событий и уникальные пользователи за 30 дней).
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class AnalyticsServiceTest {

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ---------- record ----------

    @Test
    void record_savesAllFieldsOfEvent() {
        UUID userId = insertUser();
        Long waterBodyId = insertWaterBody();
        String sessionId = "session-" + UUID.randomUUID();

        analyticsService.record(
                new EventRequest(EventType.WATER_BODY_VIEWED, sessionId, waterBodyId, "{\"screen\":\"card\"}"),
                userId);

        // читаем из базы напрямую: так видно, что реально записалось, а не что вернул Hibernate
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT user_id, event_type, water_body_id, properties->>'screen' AS screen
                FROM events WHERE session_id = ?
                """, sessionId);

        assertThat(row.get("user_id")).isEqualTo(userId);
        assertThat(row.get("event_type")).isEqualTo("WATER_BODY_VIEWED");
        assertThat(row.get("water_body_id")).isEqualTo(waterBodyId);
        assertThat(row.get("screen")).isEqualTo("card");
    }

    @Test
    void record_allowsAnonymousEventWithoutUser() {
        String sessionId = "session-" + UUID.randomUUID();

        analyticsService.record(new EventRequest(EventType.MAP_OPENED, sessionId, null, null), null);

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT user_id, water_body_id FROM events WHERE session_id = ?", sessionId);

        // анонимный посетитель определяется только по session_id
        assertThat(row.get("user_id")).isNull();
        assertThat(row.get("water_body_id")).isNull();
    }

    @Test
    void record_nullPropertiesBecomeEmptyJsonObject() {
        String sessionId = "session-" + UUID.randomUUID();

        analyticsService.record(new EventRequest(EventType.MAP_OPENED, sessionId, null, null), null);

        // в колонке properties стоит NOT NULL, поэтому вместо null сервис кладёт {}
        String properties = jdbcTemplate.queryForObject(
                "SELECT properties::text FROM events WHERE session_id = ?", String.class, sessionId);
        assertThat(properties).isEqualTo("{}");
    }

    // ---------- getSummary ----------

    @Test
    void getSummary_returnsZeroForEveryKeyWhenNoEvents() {
        jdbcTemplate.update("DELETE FROM events");

        Map<String, Object> summary = analyticsService.getSummary();

        assertThat(summary).containsOnlyKeys(
                "map_opened", "water_body_viewed", "catch_created", "catch_history_viewed",
                "unique_users_map_opened", "unique_users_catch_created");
        assertThat(summary.values()).allMatch(value -> Long.valueOf(0).equals(value));
    }

    @Test
    void getSummary_countsEventsSeparatelyByType() {
        jdbcTemplate.update("DELETE FROM events");
        UUID user = insertUser();
        // количества разные, чтобы перепутанные типы сразу дали неверное число
        insertEvents(user, EventType.MAP_OPENED, 2, 1);
        insertEvents(user, EventType.WATER_BODY_VIEWED, 1, 1);
        insertEvents(user, EventType.CATCH_CREATED, 3, 1);
        insertEvents(user, EventType.CATCH_HISTORY_VIEWED, 4, 1);

        Map<String, Object> summary = analyticsService.getSummary();

        assertThat(summary.get("map_opened")).isEqualTo(2L);
        assertThat(summary.get("water_body_viewed")).isEqualTo(1L);
        assertThat(summary.get("catch_created")).isEqualTo(3L);
        assertThat(summary.get("catch_history_viewed")).isEqualTo(4L);
    }

    @Test
    void getSummary_ignoresEventsOlderThan30Days() {
        jdbcTemplate.update("DELETE FROM events");
        UUID user = insertUser();
        insertEvents(user, EventType.MAP_OPENED, 1, 29);  // внутри окна
        insertEvents(user, EventType.MAP_OPENED, 5, 31);  // за окном

        Map<String, Object> summary = analyticsService.getSummary();

        assertThat(summary.get("map_opened")).isEqualTo(1L);
        assertThat(summary.get("unique_users_map_opened")).isEqualTo(1L);
    }

    @Test
    void getSummary_countsEachUserOnceAndSkipsAnonymous() {
        jdbcTemplate.update("DELETE FROM events");
        UUID first = insertUser();
        UUID second = insertUser();
        insertEvents(first, EventType.MAP_OPENED, 3, 1);   // один пользователь, три события
        insertEvents(second, EventType.MAP_OPENED, 1, 1);
        insertEvents(null, EventType.MAP_OPENED, 2, 1);    // анонимные посетители

        Map<String, Object> summary = analyticsService.getSummary();

        // события считаются все, а уникальные пользователи только залогиненные (COUNT DISTINCT не считает NULL)
        assertThat(summary.get("map_opened")).isEqualTo(6L);
        assertThat(summary.get("unique_users_map_opened")).isEqualTo(2L);
    }

    @Test
    void getSummary_uniqueUsersOfCatchCreatedDoNotMixWithOtherTypes() {
        jdbcTemplate.update("DELETE FROM events");
        UUID creator = insertUser();
        UUID onlyViewer = insertUser();
        insertEvents(creator, EventType.CATCH_CREATED, 2, 1);
        insertEvents(creator, EventType.MAP_OPENED, 1, 1);
        insertEvents(onlyViewer, EventType.MAP_OPENED, 1, 1);

        Map<String, Object> summary = analyticsService.getSummary();

        assertThat(summary.get("unique_users_catch_created")).isEqualTo(1L);
        assertThat(summary.get("unique_users_map_opened")).isEqualTo(2L);
    }

    // ---------- вспомогательные методы ----------

    // у events.user_id внешний ключ на users, поэтому пользователь должен существовать
    private UUID insertUser() {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO users (id, email, password_hash, display_name)
                VALUES (?, ?, 'test-hash', 'Тест')
                """, id, id + "@test.local");
        return id;
    }

    // slug уникальный, поэтому случайный: водоёмы из других тестов не конфликтуют
    private Long insertWaterBody() {
        String slug = "wb-" + UUID.randomUUID();
        return jdbcTemplate.queryForObject("""
                INSERT INTO water_bodies (name, slug, water_type, location, is_published)
                VALUES (?, ?, 'LAKE', ST_SetSRID(ST_MakePoint(61.36, 55.13), 4326), true)
                RETURNING id
                """, Long.class, slug, slug);
    }

    // created_at задаём сами, чтобы проверять окно в 30 дней; запас в день, чтобы тест не зависел от часового пояса
    private void insertEvents(UUID userId, EventType type, int count, int daysAgo) {
        for (int i = 0; i < count; i++) {
            jdbcTemplate.update("""
                    INSERT INTO events (user_id, event_type, created_at)
                    VALUES (?, ?, now() - (? * interval '1 day'))
                    """, userId, type.name(), daysAgo);
        }
    }
}