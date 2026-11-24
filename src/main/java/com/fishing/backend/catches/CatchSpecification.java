package com.fishing.backend.catches;

import jakarta.persistence.criteria.Expression;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

public class CatchSpecification {
    public static Specification<CatchEntity> byUserId(UUID userId) {
        return ((root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("userId"), userId));
    }

    public static Specification<CatchEntity> notDeleted() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.isNull(root.get("deletedAt"));
    }

    public static Specification<CatchEntity> byWaterBodyId(Long waterBodyId) {
        return (root, query, criteriaBuilder) ->
                waterBodyId == null ? null :
                        criteriaBuilder.equal(root.get("waterBodyId"), waterBodyId);
    }

    public static Specification<CatchEntity> bySpeciesId(Long speciesId) {
        return (root, query, criteriaBuilder) ->
                speciesId == null ? null :
                        criteriaBuilder.equal(root.get("speciesId"), speciesId);
    }

    public static Specification<CatchEntity> byBait(String bait) {
        return (root, query, criteriaBuilder) ->
                bait == null ? null :
                        criteriaBuilder
                                .like(criteriaBuilder
                                        .lower(root.get("bait")),
                                        "%" + bait.toLowerCase() + "%");
    }

    public static Specification<CatchEntity> bySeason(String season) {
        // сезон вычисляется из caughtAt (месяц)
        return (root, query, criteriaBuilder) -> {
            if (season == null) return null;

            // Получаем месяц из caughtAt через EXTRACT(MONTH FROM caught_at)
            Expression<Integer> month = criteriaBuilder.function(
                    "date_part",
                    Integer.class,
                    criteriaBuilder.literal("month"),
                    root.get("caughtAt")
            );

            // Определяем, какие месяцы соответствуют сезону
            List<Integer> months;
            switch (season.toLowerCase()) {
                case "winter": months = List.of(12, 1, 2); break;
                case "spring": months = List.of(3, 4, 5); break;
                case "summer": months = List.of(6, 7, 8); break;
                case "autumn": months = List.of(9, 10, 11); break;
                default: return null;
            }

            return month.in(months);
        };
    }
}