package com.zaphira.wallet.integration.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaphira.service_user.dto.request.CreateWalletRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Wallet Service Integration Test
 * 
 * Tests wallet creation and management using SQLite + Feign messaging.
 * 
 * Uses "test-sync" profile which:
 * - Uses H2 SQLite instead of PostgreSQL
 * - Uses Feign REST calls instead of Kafka
 * - Allows testing on a single machine
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test-sync")
@Slf4j
@DisplayName("Wallet Creation Integration Test (SQLite + Feign)")
public class WalletCreationE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        log.info("✅ Wallet Service test environment ready");
    }

    @Test
    @DisplayName("Should successfully create wallet for user")
    void testWalletCreationSuccess() throws Exception {
        // Given: Valid wallet creation request
        CreateWalletRequest request = CreateWalletRequest.builder()
                .userId(1L)
                .build();

        String requestBody = objectMapper.writeValueAsString(request);

        // When: POST to wallet creation endpoint
        MvcResult result = mockMvc.perform(post("/api/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                // Then: Expect success response
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.walletNumber").exists())
                .andExpect(jsonPath("$.userId").value(1))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        log.info("✅ Wallet creation response: {}", responseBody);
        
        assertThat(responseBody).contains("walletNumber");
        assertThat(responseBody).contains("userId");
    }

    @Test
    @DisplayName("Should validate userId is provided")
    void testWalletCreationValidation() throws Exception {
        // Given: Invalid request (missing userId)
        CreateWalletRequest invalidRequest = CreateWalletRequest.builder()
                .build();

        // When & Then: Expect validation error
        mockMvc.perform(post("/api/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        log.info("✅ Validation error caught for invalid request");
    }
}
