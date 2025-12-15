package com.zaphira.transaction.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaphira.common.dto.WalletDTO;
import com.zaphira.transaction.integration.wallet.FeignWalletClient;
import com.zaphira.transaction.repository.ScheduledTransactionRepository;
import com.zaphira.transaction.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ScheduledTransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ScheduledTransactionRepository scheduledTransactionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FeignWalletClient feignWalletClient;

    @BeforeEach
    void setup() {
        scheduledTransactionRepository.deleteAll();

        // Set up authenticated SecurityContext with a valid principal
        AuthenticatedUser principal = new AuthenticatedUser(1L, "test@example.com");
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                principal, null, Collections.emptyList()
        );
        SecurityContextHolder.getContext().setAuthentication(token);

        // Mock FeignWalletClient for wallet lookups
        WalletDTO wallet1 = WalletDTO.builder()
                .id(1L)
                .userId(1L)
                .walletNumber("W1")
                .balance(BigDecimal.valueOf(5000.0))
                .active(true)
                .build();

        WalletDTO wallet2 = WalletDTO.builder()
                .id(2L)
                .userId(2L)
                .walletNumber("W2")
                .balance(BigDecimal.valueOf(3000.0))
                .active(true)
                .build();

        given(feignWalletClient.getWalletByNumber("W1")).willReturn(wallet1);
        given(feignWalletClient.getWalletByNumber("W2")).willReturn(wallet2);
    }

    private RequestPostProcessor authenticated() {
        return request -> request;
    }

    @Test
    void createAndGetScheduledTransaction_shouldWork() throws Exception {
        String payload = """
                {
                  "senderWalletNumber":"W1",
                  "receiverWalletNumber":"W2",
                  "amount":100.0,
                  "currency":"XOF",
                  "type":"P2P_TRANSFER",
                  "channel":"MOBILE",
                  "description":"Scheduled test",
                  "requestedBy":"integration-test",
                  "scheduledFor":"%s"
                }
                """.formatted(LocalDateTime.now().plusMinutes(5).toString());

        String createdBody = mockMvc.perform(post("/api/transactions/scheduled")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .with(authenticated()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long id = objectMapper.readTree(createdBody).get("id").asLong();

        mockMvc.perform(get("/api/transactions/scheduled/{id}", id)
                        .with(authenticated()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is((int) id)));

        mockMvc.perform(get("/api/transactions/scheduled")
                        .with(authenticated()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }
}


