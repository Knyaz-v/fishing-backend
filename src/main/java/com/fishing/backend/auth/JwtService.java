package com.fishing.backend.auth;

import com.fishing.backend.user.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

// Выдача JWT после регистрации и входа. Проверяет токены Spring Security (JwtDecoder в JwtConfig).
@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final Duration ttl;

    public JwtService(JwtEncoder jwtEncoder, @Value("${app.jwt.ttl-hours}") long ttlHours) {
        this.jwtEncoder = jwtEncoder;
        this.ttl = Duration.ofHours(ttlHours);
    }

    public String createToken(UUID userId, Role role) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                // email в токен не кладём: данные токена читаются без ключа, это персональные данные
                .claim("role", role.name())
                .build();
        // алгоритм указываем явно: по умолчанию в заголовке был бы RS256, а наш ключ — для HS256
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
