package com.fishing.backend.auth;

// Бизнес-ошибка регистрации; в HTTP 409 её переводит GlobalExceptionHandler.
public class EmailAlreadyUsedException extends RuntimeException {
}
