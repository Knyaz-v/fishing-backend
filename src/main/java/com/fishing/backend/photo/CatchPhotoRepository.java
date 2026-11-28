package com.fishing.backend.photo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CatchPhotoRepository extends JpaRepository<CatchPhotoEntity, UUID> {
    List<CatchPhotoEntity> findByCatchIdOrderBySortOrderAsc(UUID catchId);
}
