package com.fishing.backend.analytics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface EventRepository extends JpaRepository<EventEntity, Long> {

    // Считаем уникальных пользователей для типа события (для агрегатов)
    @Query("SELECT COUNT(DISTINCT e.userId) FROM EventEntity e WHERE e.eventType = :eventType AND e.createdAt >= :from")
    long countDistinctUsersByEventTypeSince(String eventType, LocalDateTime from);

    // Считаем общее количество событий типа за период
    @Query("SELECT COUNT(e) FROM EventEntity e WHERE e.eventType = :eventType AND e.createdAt >= :from")
    long countByEventTypeSince(String eventType, LocalDateTime from);
}