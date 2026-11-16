package com.fishing.backend.photo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "catch_photos")
@Getter
@Setter
public class CatchPhotoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "catch_id", nullable = false)
    private UUID catchId;

    @Column(name = "storage_key", nullable = false, unique = true)
    private String storageKey;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    private Integer width;

    private Integer height;

    @Column(name = "sort_order", nullable = false)
    private Short sortOrder = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
