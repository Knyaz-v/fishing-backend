package com.fishing.backend.auth;

import com.fishing.backend.auth.dto.AuthResponse;
import com.fishing.backend.auth.dto.LoginRequest;
import com.fishing.backend.auth.dto.RegisterRequest;
import com.fishing.backend.user.UserEntity;
import com.fishing.backend.user.UserRepository;
import com.fishing.backend.user.dto.UserProfile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException();
        }
        UserEntity user = new UserEntity(email, passwordEncoder.encode(request.password()), request.displayName().trim());
        userRepository.save(user);
        return toAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(InvalidCredentialsException::new);
        // заблокированный пользователь и неверный пароль дают ту же ошибку, что и неизвестный email
        if (!user.isActive() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return toAuthResponse(user);
    }

    // в базе email хранится в нижнем регистре: уникальный индекс построен по lower(email)
    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private AuthResponse toAuthResponse(UserEntity user) {
        UserProfile profile = new UserProfile(user.getId(), user.getEmail(), user.getDisplayName());
        String token = jwtService.createToken(user.getId(), user.getRole());
        return new AuthResponse(token, profile);
    }
}
