package com.fishing.backend.weather;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OpenMeteoWeatherServiceTest {

    @Test
    void getWeatherSnapshot_returnsNullOnFailure() {
        OpenMeteoWeatherService service = new OpenMeteoWeatherService();
        // Неверные координаты (за пределами Земли) — API вернёт ошибку
        String result = service.getWeatherSnapshot(999, 999, LocalDateTime.now());
        assertThat(result).isNull();
    }
}