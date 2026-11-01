# Изменения для команды

Сюда записываем изменения, после которых другому участнику нужно что-то сделать или знать:
контракт API, общий код, настройки окружения. Новые записи сверху. Подробности — в PR по ссылке.

## 2026-11-01 — Spring Security и регистрация (PR #15)

**Фронтенду**
- `POST /api/v1/auth/register`, тело `{"email": "...", "password": "...", "displayName": "..."}`
  → `201` и профиль `{"id": "<uuid>", "email": "...", "displayName": "..."}`. Email возвращается в нижнем регистре.
  JWT пока не выдаётся: он появится в следующем PR вместе со входом `POST /api/v1/auth/login`.
- Ограничения: email — корректный адрес до 255 символов; пароль 8–72 символа; `displayName` не пустой, до 100 символов.
  Нарушение → `400` ProblemDetail (общий текст, без указания поля).
- Email уже зарегистрирован (регистр букв не важен) → `409` ProblemDetail.

**Всем**
- Подключён Spring Security. Без входа доступны только: `POST /api/v1/auth/**`, `GET /api/v1/water-bodies`,
  `GET /api/v1/water-bodies/{id}`, Swagger, `/actuator/health`. Остальные адреса отвечают `403`,
  пока не появится JWT (следующий PR, тогда ответ для анонима станет `401`).

**Бэкенду**
- Эндпоинты уловов `/api/v1/catches` после слияния закрыты для анонима. Новый открытый эндпоинт нужно явно
  добавить в `SecurityConfig.securityFilterChain`.
- В MockMvc-тестах закрытых эндпоинтов добавляйте `@WithMockUser` (зависимость `spring-boot-starter-security-test`
  уже подключена), пример — `ErrorHandlingTest`.

## 2026-10-26 — карточка водоёма (PR #14)

**Фронтенду**
- Новый эндпоинт `GET /api/v1/water-bodies/{id}` — карточка опубликованного водоёма. Пример ответа:
  ```json
  {
    "id": 1, "name": "Озеро Смолино", "waterType": "LAKE", "lat": 55.13, "lon": 61.36,
    "description": "Озеро в черте города", "district": "Челябинск",
    "accessType": "FREE", "accessNotes": "Бесплатно",
    "hasParking": true, "parkingNotes": "У пляжа",
    "rulesNotes": "Весной с лодок нельзя", "verifiedAt": "2026-10-26T05:33:25.536676Z"
  }
  ```
- `accessType`: `FREE`, `PAID`, `PERMIT`, `UNKNOWN`. `hasParking` может быть `null` — «неизвестно»,
  это не то же самое, что `false`. Текстовые поля и `verifiedAt` тоже могут быть `null`.
  `verifiedAt` — ISO-8601 в UTC, доли секунды могут быть, а могут отсутствовать.
- Водоём не найден или не опубликован → `404` ProblemDetail; нечисловой `id` → `400`.
- Рыба, последние уловы и погода появятся в карточке позже, новыми полями.
  
## 2026-10-25 — список водоёмов по области карты (PR #12)

**Фронтенду**
- `GET /api/v1/water-bodies` требует параметр `bbox=minLon,minLat,maxLon,maxLat`:
  сначала долгота, градусы WGS 84. Пример: `bbox=61.0,54.9,61.8,55.4`.
- Без `bbox` или с некорректным значением ответ `400` в формате ProblemDetail
  (`application/problem+json`), текст ошибки в поле `detail`.

**Бэкенду**
- `WaterBodyRepository.findAllByIsPublishedTrue` удалён; водоёмы для карты — `findPublishedInBbox`.

## 2026-10-22 — CORS (PR #11)

**Фронтенду**
- Браузерные запросы к `/api/**` разрешены только с origin из `CORS_ALLOWED_ORIGIN`
  (по умолчанию `http://localhost:5173`). Если dev-сервер фронтенда на другом порту,
  этот адрес нужно прописать в `.env` бэкенда.
- Разрешены методы `GET, POST, PUT, PATCH, DELETE` и заголовки `Content-Type`, `Authorization`.
