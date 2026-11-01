package com.fishing.backend.auth;

import com.fishing.backend.auth.dto.AuthResponse;
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
        // в базе email хранится в нижнем регистре: уникальный индекс построен по lower(email)
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException();
        }
        UserEntity user = new UserEntity(email, passwordEncoder.encode(request.password()), request.displayName().trim());
        userRepository.save(user);

        UserProfile profile = new UserProfile(user.getId(), user.getEmail(), user.getDisplayName());
        String token = jwtService.createToken(user.getId(), user.getRole());
        return new AuthResponse(token, profile);
    }
}
