package com.fishing.backend.catches.dto;

import java.time.LocalDate;

public record CatchFilterRequest(
        Long waterBodyId,
        Long speciesId,
        String bait,
        String season,
        LocalDate aroundDate,
        Integer windowDays
) { }
