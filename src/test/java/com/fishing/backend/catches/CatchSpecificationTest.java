package com.fishing.backend.catches;

import com.fishing.backend.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

// Проверяет, что каждый фильтр CatchSpecification превращается в правильный SQL-запрос к настоящей БД.
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class CatchSpecificationTest {

    // ожидаемые месяцы записаны в тесте отдельно от спецификации: иначе тест сверял бы код сам с собой
    private static final Map<String, List<Integer>> SEASON_MONTHS = Map.of(
            "winter", List.of(12, 1, 2),
            "spring", List.of(3, 4, 5),
            "summer", List.of(6, 7, 8),
            "autumn", List.of(9, 10, 11)
    );

    @Autowired
    private CatchRepository catchRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // JUnit создаёт новый объект на каждый тест, поэтому у каждого теста свой пользователь
    // и чужие данные в базе ему не мешают
    private final UUID userId = UUID.randomUUID();

    @Test
    void byUserId_returnsOnlyOwnCatches() {
        UUID own = insertCatch(userId, null, null, "2026-07-15 12:00:00+00", null, false);
        insertCatch(UUID.randomUUID(), null, null, "2026-07-15 12:00:00+00", null, false);

        List<CatchEntity> result = catchRepository.findAll(CatchSpecification.byUserId(userId));

        assertThat(ids(result)).containsExactly(own);
    }

    @Test
    void notDeleted_skipsSoftDeletedCatches() {
        UUID alive = insertCatch(userId, null, null, "2026-07-15 12:00:00+00", null, false);
        insertCatch(userId, null, null, "2026-07-16 12:00:00+00", null, true);

        assertThat(findIds(CatchSpecification.notDeleted())).containsExactly(alive);
    }

    @Test
    void byWaterBodyId_returnsCatchesOfThatWaterBody() {
        Long smolino = insertWaterBody();
        Long uvildy = insertWaterBody();
        UUID inSmolino = insertCatch(userId, smolino, null, "2026-07-15 12:00:00+00", null, false);
        insertCatch(userId, uvildy, null, "2026-07-15 12:00:00+00", null, false);

        assertThat(findIds(CatchSpecification.byWaterBodyId(smolino))).containsExactly(inSmolino);
    }

    @Test
    void byWaterBodyId_nullMeansNoFilter() {
        Long waterBodyId = insertWaterBody();
        UUID withWater = insertCatch(userId, waterBodyId, null, "2026-07-15 12:00:00+00", null, false);
        UUID withoutWater = insertCatch(userId, null, null, "2026-07-16 12:00:00+00", null, false);

        assertThat(findIds(CatchSpecification.byWaterBodyId(null)))
                .containsExactlyInAnyOrder(withWater, withoutWater);
    }

    @Test
    void bySpeciesId_returnsCatchesOfThatSpecies() {
        Long pike = insertSpecies();
        Long perch = insertSpecies();
        UUID pikeCatch = insertCatch(userId, null, pike, "2026-07-15 12:00:00+00", null, false);
        insertCatch(userId, null, perch, "2026-07-15 12:00:00+00", null, false);

        assertThat(findIds(CatchSpecification.bySpeciesId(pike))).containsExactly(pikeCatch);
    }

    @Test
    void bySpeciesId_nullMeansNoFilter() {
        Long species = insertSpecies();
        UUID withSpecies = insertCatch(userId, null, species, "2026-07-15 12:00:00+00", null, false);
        UUID withoutSpecies = insertCatch(userId, null, null, "2026-07-16 12:00:00+00", null, false);

        assertThat(findIds(CatchSpecification.bySpeciesId(null)))
                .containsExactlyInAnyOrder(withSpecies, withoutSpecies);
    }

    @Test
    void byBait_findsSubstringIgnoringCase() {
        UUID spinner = insertCatch(userId, null, null, "2026-07-15 12:00:00+00", "Вертушка Mepps №3", false);
        insertCatch(userId, null, null, "2026-07-16 12:00:00+00", "Червь", false);

        // ищем часть слова другим регистром: и в базе, и в запросе слова приводятся к нижнему регистру
        assertThat(findIds(CatchSpecification.byBait("ВЕРТУШ"))).containsExactly(spinner);
    }

    @Test
    void byBait_skipsCatchesWithoutBait() {
        insertCatch(userId, null, null, "2026-07-15 12:00:00+00", null, false);
        UUID worm = insertCatch(userId, null, null, "2026-07-16 12:00:00+00", "Червь", false);

        assertThat(findIds(CatchSpecification.byBait("червь"))).containsExactly(worm);
    }

    @Test
    void byBait_nullMeansNoFilter() {
        UUID withBait = insertCatch(userId, null, null, "2026-07-15 12:00:00+00", "Червь", false);
        UUID withoutBait = insertCatch(userId, null, null, "2026-07-16 12:00:00+00", null, false);

        assertThat(findIds(CatchSpecification.byBait(null)))
                .containsExactlyInAnyOrder(withBait, withoutBait);
    }

    @ParameterizedTest
    @ValueSource(strings = {"winter", "spring", "summer", "autumn"})
    void bySeason_returnsCatchesOfThoseMonthsOnly(String season) {
        // по одному улову на каждый месяц: тест проверяет сразу все 12 месяцев
        Map<Integer, UUID> catchByMonth = insertCatchForEveryMonth();

        Set<UUID> expected = SEASON_MONTHS.get(season).stream()
                .map(catchByMonth::get)
                .collect(Collectors.toSet());

        assertThat(findIds(CatchSpecification.bySeason(season))).isEqualTo(expected);
    }

    @Test
    void bySeason_ignoresCaseOfSeasonName() {
        Map<Integer, UUID> catchByMonth = insertCatchForEveryMonth();

        assertThat(findIds(CatchSpecification.bySeason("Summer")))
                .containsExactlyInAnyOrder(catchByMonth.get(6), catchByMonth.get(7), catchByMonth.get(8));
    }

    @Test
    void bySeason_nullMeansNoFilter() {
        Map<Integer, UUID> catchByMonth = insertCatchForEveryMonth();

        assertThat(findIds(CatchSpecification.bySeason(null)))
                .hasSameSizeAs(catchByMonth.values());
    }

    @Test
    void bySeason_unknownSeasonMeansNoFilter() {
        Map<Integer, UUID> catchByMonth = insertCatchForEveryMonth();

        // сейчас неизвестный сезон молча игнорируется; если решите возвращать 400, этот тест нужно поменять
        assertThat(findIds(CatchSpecification.bySeason("monsoon")))
                .hasSameSizeAs(catchByMonth.values());
    }

    @Test
    void specifications_canBeCombined() {
        Long smolino = insertWaterBody();
        Long uvildy = insertWaterBody();
        Long pike = insertSpecies();

        UUID match = insertCatch(userId, smolino, pike, "2026-07-15 12:00:00+00", null, false);
        insertCatch(userId, uvildy, pike, "2026-07-15 12:00:00+00", null, false);   // другой водоём
        insertCatch(userId, smolino, pike, "2026-01-15 12:00:00+00", null, false);  // другой сезон
        insertCatch(userId, smolino, pike, "2026-07-16 12:00:00+00", null, true);   // удалён

        Specification<CatchEntity> spec = CatchSpecification.notDeleted()
                .and(CatchSpecification.byWaterBodyId(smolino))
                .and(CatchSpecification.bySpeciesId(pike))
                .and(CatchSpecification.bySeason("summer"));

        assertThat(findIds(spec)).containsExactly(match);
    }

    // ---------- вспомогательные методы ----------

    // всегда добавляем byUserId, чтобы в результат не попали улова из других тестов
    private Set<UUID> findIds(Specification<CatchEntity> spec) {
        return ids(catchRepository.findAll(CatchSpecification.byUserId(userId).and(spec)));
    }

    private Set<UUID> ids(List<CatchEntity> catches) {
        return catches.stream().map(CatchEntity::getId).collect(Collectors.toSet());
    }

    // даты берём из середины месяца и полудня: граница месяца зависела бы от часового пояса сессии БД
    private Map<Integer, UUID> insertCatchForEveryMonth() {
        Map<Integer, UUID> catchByMonth = new HashMap<>();
        for (int month = 1; month <= 12; month++) {
            String caughtAt = String.format("2026-%02d-15 12:00:00+00", month);
            catchByMonth.put(month, insertCatch(userId, null, null, caughtAt, null, false));
        }
        return catchByMonth;
    }

    // тестовые данные кладём прямо в базу: так тест проверяет только спецификацию, а не создание улова
    private UUID insertCatch(UUID owner, Long waterBodyId, Long speciesId,
                             String caughtAt, String bait, boolean deleted) {
        // у catches.user_id внешний ключ на users, поэтому владелец улова должен существовать;
        // ON CONFLICT нужен, потому что один пользователь получает несколько уловов
        jdbcTemplate.update("""
                INSERT INTO users (id, email, password_hash, display_name)
                VALUES (?, ?, 'test-hash', 'Тест')
                ON CONFLICT (id) DO NOTHING
                """, owner, owner + "@test.local");

        UUID id = UUID.randomUUID();
        String deletedAt = deleted ? "2026-08-01 12:00:00+00" : null;
        jdbcTemplate.update("""
                INSERT INTO catches (id, user_id, water_body_id, species_id, caught_at, bait, deleted_at)
                VALUES (?, ?, ?, ?, ?::timestamptz, ?, ?::timestamptz)
                """, id, owner, waterBodyId, speciesId, caughtAt, bait, deletedAt);
        return id;
    }

    // slug уникальный, поэтому берём случайный: водоёмы из других тестов не конфликтуют
    private Long insertWaterBody() {
        String slug = "wb-" + UUID.randomUUID();
        return jdbcTemplate.queryForObject("""
                INSERT INTO water_bodies (name, slug, water_type, location, is_published)
                VALUES (?, ?, 'LAKE', ST_SetSRID(ST_MakePoint(61.36, 55.13), 4326), true)
                RETURNING id
                """, Long.class, slug, slug);
    }

    private Long insertSpecies() {
        String slug = "fish-" + UUID.randomUUID();
        return jdbcTemplate.queryForObject("""
                INSERT INTO fish_species (name_ru, slug, is_protected)
                VALUES (?, ?, false)
                RETURNING id
                """, Long.class, slug, slug);
    }
}