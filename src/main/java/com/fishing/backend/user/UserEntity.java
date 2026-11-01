package com.fishing.backend.user;

import jakarta.persistence.*;

import java.util.UUID;

// Сущность таблицы users. created_at и updated_at не отображены: их ставит база (DEFAULT now() и триггер).
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String email;

    private String passwordHash; // BCrypt-хэш, пароль в открытом виде не храним

    private String displayName;

    @Enumerated(EnumType.STRING)
    private Role role;

    private boolean isActive;

    // для Hibernate: он создаёт объект при чтении из базы и заполняет поля сам
    protected UserEntity() {
    }

    public UserEntity(String email, String passwordHash, String displayName) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.role = Role.USER;
        this.isActive = true;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
}
