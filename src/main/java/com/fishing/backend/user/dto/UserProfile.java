package com.fishing.backend.user.dto;

import java.util.UUID;

// Профиль пользователя для клиента: без пароля и служебных полей.
public record UserProfile(UUID id, String email, String displayName) {
}
