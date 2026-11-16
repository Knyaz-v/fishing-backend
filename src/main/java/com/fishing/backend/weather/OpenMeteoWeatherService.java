package com.fishing.backend.weather;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class OpenMeteoWeatherService implements WeatherService {

    private static final String ARCHIVE_URL = "https://archive-api.open-meteo.com/v1/archive";

    @Cacheable(value = "weather", key = "#lat + ',' + #lon + ',' + #dateTime.toLocalDate()")
    @Override
    public String getWeatherSnapshot(double lat, double lon, LocalDateTime dateTime) {
        try {
            String date = dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE);

            RestClient client = RestClient.builder().baseUrl(ARCHIVE_URL).build();

            return client.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("latitude", lat)
                            .queryParam("longitude", lon)
                            .queryParam("hourly", "temperature_2m,wind_speed_10m,pressure_msl")
                            .queryParam("start_date", date)
                            .queryParam("end_date", date)
                            .queryParam("timezone", "auto")
                            .build())
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            return null;
        }
    }
}