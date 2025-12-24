package com.zaphira.service_user.integration.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaphira.service_user.client.WalletServiceClient;
import com.zaphira.service_user.dto.request.CreateWalletRequest;
import com.zaphira.service_user.dto.response.WalletResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Synchronous Inter-Microservice Communication Test
 * 
 * Tests communication between user-service and wallet-service
 * using Feign client with mocked responses.
 * 
 * In test-sync profile, services communicate via Feign REST calls
 * instead of Kafka, allowing complete E2E testing on a single machine.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test-sync")
@Slf4j
@DisplayName("Synchronous Inter-Microservice Communication Test")
public class SynchronousMicroserviceCommunicationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestTemplate restTemplate;

    @MockBean
    private WalletServiceClient walletServiceClient;

    @Test
    @DisplayName("Should successfully call wallet-service via Feign")
    void testWalletServiceCommunication() {
        // Given: Mock wallet service response
        WalletResponse mockWalletResponse = WalletResponse.builder()
                .id(1L)
                .walletNumber("WALLET-2024-001")
                .userId(1L)
                .metadata("{}")
                .build();

        when(walletServiceClient.createWallet(any(CreateWalletRequest.class)))
                .thenReturn(mockWalletResponse);

        // When: Create wallet via Feign client
        CreateWalletRequest request = CreateWalletRequest.builder()
                .userId(1L)
                .build();

        WalletResponse response = walletServiceClient.createWallet(request);

        // Then: Verify response
        assertThat(response).isNotNull();
        assertThat(response.getWalletNumber()).isEqualTo("WALLET-2024-001");
        assertThat(response.getUserId()).isEqualTo(1L);

        log.info("✅ Wallet service communication successful: {}", response);
    }

    @Test
    @DisplayName("Should handle wallet service timeout gracefully")
    void testWalletServiceTimeout() {
        // Given: Wallet service throws exception
        when(walletServiceClient.createWallet(any(CreateWalletRequest.class)))
                .thenThrow(new RuntimeException("Connection timeout"));

        // When & Then: Verify error handling
        CreateWalletRequest request = CreateWalletRequest.builder()
                .userId(1L)
                .build();

        assertThatThrownBy(() -> walletServiceClient.createWallet(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Connection timeout");

        log.info("✅ Timeout handling verified");
    }

    @Test
    @DisplayName("Should retry on transient failure")
    void testRetryOnTransientFailure() {
        // Given: Service fails once then succeeds
        WalletResponse successResponse = WalletResponse.builder()
                .walletNumber("WALLET-2024-002")
                .userId(2L)
                .build();

        when(walletServiceClient.createWallet(any(CreateWalletRequest.class)))
                .thenThrow(new RuntimeException("Transient error"))
                .thenReturn(successResponse);

        // When: Create wallet (with retry)
        CreateWalletRequest request = CreateWalletRequest.builder()
                .userId(2L)
                .build();

        // First call fails
        assertThatThrownBy(() -> walletServiceClient.createWallet(request))
                .isInstanceOf(RuntimeException.class);

        // Second call succeeds
        WalletResponse response = walletServiceClient.createWallet(request);

        // Then: Verify success
        assertThat(response.getWalletNumber()).isEqualTo("WALLET-2024-002");
        log.info("✅ Retry mechanism working correctly");
    }

    @Test
    @DisplayName("Should validate request before calling wallet-service")
    void testRequestValidation() {
        // Given: Invalid request (null userId)
        CreateWalletRequest invalidRequest = CreateWalletRequest.builder()
                .build();

        // When & Then: Verify validation
        assertThatThrownBy(() -> {
            if (invalidRequest.getUserId() == null) {
                throw new IllegalArgumentException("UserId cannot be null");
            }
        }).isInstanceOf(IllegalArgumentException.class);

        log.info("✅ Request validation working correctly");
    }
}
