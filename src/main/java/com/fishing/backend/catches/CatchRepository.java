package com.fishing.backend.catches;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface CatchRepository extends JpaRepository<CatchEntity, UUID>,
        JpaSpecificationExecutor<CatchEntity> {
    List<CatchEntity> findByUserIdAndDeletedAtIsNull(UUID userId);
}