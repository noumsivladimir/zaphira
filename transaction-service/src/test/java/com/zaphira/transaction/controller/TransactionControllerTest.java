package com.zaphira.transaction.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaphira.common.dto.WalletDTO;
import com.zaphira.transaction.integration.wallet.FeignWalletClient;
import com.zaphira.transaction.model.Transaction;
import com.zaphira.transaction.model.enums.TransactionChannel;
import com.zaphira.transaction.model.enums.TransactionStatus;
import com.zaphira.transaction.model.enums.TransactionType;
import com.zaphira.transaction.repository.AuthorizationRequestRepository;
import com.zaphira.transaction.repository.TransactionRepository;
import com.zaphira.transaction.repository.TransactionStateHistoryRepository;
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
import com.zaphira.transaction.security.AuthenticatedUser;

import java.math.BigDecimal;
import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionStateHistoryRepository stateHistoryRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthorizationRequestRepository authorizationRequestRepository;

    @MockBean
    private FeignWalletClient feignWalletClient;

    @BeforeEach
    void setup() {
        authorizationRequestRepository.deleteAll();
        stateHistoryRepository.deleteAll();
        transactionRepository.deleteAll();

        // Set up authenticated SecurityContext with a valid principal
        AuthenticatedUser principal = new AuthenticatedUser(1L, "test@example.com");
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                principal, null, Collections.emptyList()
        );
        SecurityContextHolder.getContext().setAuthentication(token);

        // Mock FeignWalletClient para retourner des WalletDTO
        WalletDTO wallet1 = WalletDTO.builder()
                .id(1L)
                .userId(1L)
                .walletNumber("W1")
                .availableBalance(BigDecimal.valueOf(5000.0))
                
                .build();

        WalletDTO wallet2 = WalletDTO.builder()
                .id(2L)
                .userId(2L)
                .walletNumber("W2")
                .availableBalance(BigDecimal.valueOf(3000.0))
                
                .build();

        given(feignWalletClient.getWalletByNumber("W1"))
                .willReturn(wallet1);
        given(feignWalletClient.getWalletByNumber("W2"))
                .willReturn(wallet2);
    }

    private RequestPostProcessor authenticated() {
        AuthenticatedUser principal = new AuthenticatedUser(1L, "test@example.com");
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                principal, null, Collections.emptyList()
        );
        return authentication(token);
    }

    @Test
    void createTransaction_shouldReturn201() throws Exception {
        String payload = """
                {
                  "senderWalletNumber":"W1",
                  "receiverWalletNumber":"W2",
                  "amount":100.0,
                  "currency":"XOF",
                  "type":"P2P_TRANSFER",
                  "channel":"MOBILE",
                  "description":"Test payment",
                  "processInstantly":true,
                  "requestedBy":"integration-test"
                }
                """;

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .with(authenticated()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.senderWalletNumber", is("W1")))
                .andExpect(jsonPath("$.status", is("COMPLETED")));
    }

    @Test
    void getTransaction_shouldReturn200() throws Exception {
        Transaction tx = Transaction.builder()
                .senderWalletNumber("W1")
                .receiverWalletNumber("W2")
                .amount(BigDecimal.valueOf(50.0))
                .currency("XOF")
                .type(TransactionType.P2P_TRANSFER)
                .status(TransactionStatus.INITIATED)
                .channel(TransactionChannel.MOBILE)
                .build();
        Transaction saved = transactionRepository.save(tx);

        mockMvc.perform(get("/api/transactions/{id}", saved.getId())
                        .with(authenticated()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount", is(50.0)));
    }

    @Test
    void updateStatus_shouldReturn200() throws Exception {
        Transaction tx = Transaction.builder()
                .senderWalletNumber("W1")
                .receiverWalletNumber("W2")
                .amount(BigDecimal.valueOf(50.0))
                .currency("XOF")
                .type(TransactionType.P2P_TRANSFER)
                .status(TransactionStatus.INITIATED)
                .channel(TransactionChannel.MOBILE)
                .build();
        Transaction saved = transactionRepository.save(tx);

        mockMvc.perform(put("/api/transactions/{id}/status", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status":"PENDING",
                                  "changedBy":"integration-test",
                                  "reason":"test update"
                                }
                                """)
                        .with(authenticated()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PENDING")));
    }

    @Test
    void cancelTransaction_shouldReturn200() throws Exception {
        Transaction tx = Transaction.builder()
                .senderWalletNumber("W1")
                .receiverWalletNumber("W2")
                .amount(BigDecimal.valueOf(50.0))
                .currency("XOF")
                .type(TransactionType.P2P_TRANSFER)
                .status(TransactionStatus.PENDING)
                .channel(TransactionChannel.MOBILE)
                .build();
        Transaction saved = transactionRepository.save(tx);

        mockMvc.perform(put("/api/transactions/{id}/cancel", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status":"CANCELLED",
                                  "changedBy":"integration-test",
                                  "reason":"cancel test"
                                }
                                """)
                        .with(authenticated()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")));
    }

    @Test
    void authorizationFlow_shouldCompleteTransaction() throws Exception {
        String payload = """
                {
                  "senderWalletNumber":"W1",
                  "receiverWalletNumber":"W2",
                  "amount":2000.0,
                  "currency":"XOF",
                  "type":"P2P_TRANSFER",
                  "channel":"MOBILE",
                  "description":"High value",
                  "processInstantly":true,
                  "requestedBy":"integration-test"
                }
                """;

        String createResponse = mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .with(authenticated()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long id = objectMapper.readTree(createResponse).get("id").asLong();

        String authBody = mockMvc.perform(get("/api/transactions/{id}/authorization", id)
                        .with(authenticated()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String code = objectMapper.readTree(authBody).get("challengeCode").asText();

        mockMvc.perform(post("/api/transactions/{id}/authorize", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(authenticated())
                        .content("""
                                {
                                  "method":"OTP",
                                  "code":"%s",
                                  "authorizedBy":"integration-test"
                                }
                                """.formatted(code)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")));
    }
}
