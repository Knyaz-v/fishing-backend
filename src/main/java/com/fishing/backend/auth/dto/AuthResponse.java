package com.fishing.backend.auth.dto;

import com.fishing.backend.user.dto.UserProfile;

// Ответ регистрации и входа: токен для заголовка Authorization и профиль, чтобы клиенту не делать второй запрос.
public record AuthResponse(String accessToken, UserProfile user) {
}
