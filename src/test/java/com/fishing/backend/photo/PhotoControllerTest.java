package com.fishing.backend.photo;

import com.fishing.backend.TestcontainersConfiguration;
import com.fishing.backend.catches.CatchEntity;
import com.fishing.backend.catches.CatchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class PhotoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CatchRepository catchRepo;

    @Test
    void uploadPhoto_unknownCatch_returns404() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID catchId = UUID.randomUUID();

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "fake image content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/catches/" + catchId + "/photos")
                        .file(file)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt.subject(userId.toString()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void uploadPhoto_otherUser_returns403() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();

        // Создаём улов от ownerId
        CatchEntity catchEntity = new CatchEntity();
        catchEntity.setUserId(ownerId);
        catchEntity.setCaughtAt(LocalDateTime.now());
        catchEntity.setVisibility("PRIVATE");
        catchRepo.save(catchEntity);

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "fake image content".getBytes()
        );

        // Пытаемся загрузить фото от otherId
        mockMvc.perform(multipart("/api/v1/catches/" + catchEntity.getId() + "/photos")
                        .file(file)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt.subject(otherId.toString()))))
                .andExpect(status().isForbidden());
    }
}