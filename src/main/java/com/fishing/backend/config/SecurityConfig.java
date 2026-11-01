package com.fishing.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

// Кто куда может зайти. Открыто то, что по контракту доступно без входа (CLAUDE.md, раздел 6),
// всё остальное требует аутентификации.
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF защищает cookie-сессии; у нас токен в заголовке Authorization, cookie нет
                .csrf(csrf -> csrf.disable())
                // правила CORS Security берёт из CorsConfig (WebMvcConfigurer)
                .cors(Customizer.withDefaults())
                // сервер не хранит сессию: пользователь определяется по токену в каждом запросе
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/water-bodies", "/api/v1/water-bodies/*").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated());
        return http.build();
    }
}
