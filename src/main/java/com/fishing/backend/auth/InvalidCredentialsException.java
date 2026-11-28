package com.fishing.backend.auth;

// Неверный email, пароль или заблокированный пользователь — для клиента одна ошибка; в 401 её переводит GlobalExceptionHandler.
public class InvalidCredentialsException extends RuntimeException {
}
