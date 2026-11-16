package com.fishing.backend.user;

import com.fishing.backend.user.dto.UserProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Профиль текущего пользователя")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "200", description = "Профиль")
    @ApiResponse(responseCode = "401", description = "Нет токена или токен недействителен")
    public UserProfile me(@AuthenticationPrincipal Jwt jwt) {
        // id берём из подписанного токена (claim sub), а не из запроса: подставить чужой id нельзя
        UUID userId = UUID.fromString(jwt.getSubject());
        // токен действителен, но пользователя уже нет в базе — для клиента это тоже «нужно войти заново»
        return userService.findProfile(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Требуется вход"));
    }
}
