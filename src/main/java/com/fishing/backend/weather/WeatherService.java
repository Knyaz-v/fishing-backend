package com.fishing.backend.weather;

import java.time.LocalDateTime;

public interface WeatherService {
    /**
     * Получает снимок погоды для указанных координат и времени.
     * Возвращает JSON-строку (для сохранения в catches.weather) или null, если не удалось.
     */
    String getWeatherSnapshot(double lat, double lon, LocalDateTime dateTime);
}
