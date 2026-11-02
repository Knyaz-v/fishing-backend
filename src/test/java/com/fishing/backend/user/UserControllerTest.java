package com.fishing.backend.user;

import com.fishing.backend.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Профиль по токену: с действительным JWT — 200, без токена или с подделанным — 401 ProblemDetail.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class UserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUsers() {
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void returnsProfileForValidToken() throws Exception {
        String token = registerAndGetToken();

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("me@example.com"))
                .andExpect(jsonPath("$.displayName").value("Миша"));
    }

    @Test
    void rejectsRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.detail").value("Требуется вход"));
    }

    @Test
    void rejectsTamperedToken() throws Exception {
        String token = registerAndGetToken();
        // лишний символ в подписи: данные те же, но подпись больше не сходится
        String tampered = token + "x";

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType("application/problem+json"));
    }

    private String registerAndGetToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "me@example.com", "password": "secret123", "displayName": "Миша" }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }
}
