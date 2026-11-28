package com.fishing.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// Без ограничений длины пароля: при входе не подсказываем правила, просто сверяем с хэшем.
public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
}
