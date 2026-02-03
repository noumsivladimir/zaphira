package com.zaphira.transaction.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaphira.transaction.dto.requests.TransferRequest;
import com.zaphira.transaction.dto.requests.DepositRequest;
import com.zaphira.transaction.dto.requests.WithdrawalRequest;
import com.zaphira.transaction.dto.requests.PaymentRequest;
import com.zaphira.transaction.model.enums.TransactionStatus;
import com.zaphira.transaction.model.enums.TransactionType;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Tests d'intégration pour Transaction Service - LOT 1
 * Valide les endpoints core du système transactionnel
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Transaction Service Integration Tests - LOT 1")
class TransactionServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String createdTransactionRef;

    @Nested
    @DisplayName("LOT 1 - P2P Transfer Tests")
    class TransferTests {

        @Test
        @Order(1)
        @DisplayName("Should create P2P transfer successfully")
        @WithMockUser(username = "1", roles = {"REGULAR"})
        void testCreateTransfer() throws Exception {
            TransferRequest request = TransferRequest.builder()
                    .senderWalletNumber("12345678")
                    .receiverWalletNumber("87654321")
                    .amount(BigDecimal.valueOf(1000))
                    .description("Test P2P Transfer")
                    .build();

            MvcResult result = mockMvc.perform(post("/api/transactions/transfer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.transactionReference").exists())
                    .andExpect(jsonPath("$.type").value(TransactionType.TRANSFER.name()))
                    .andExpect(jsonPath("$.amount").value(1000))
                    .andExpect(jsonPath("$.status").value(TransactionStatus.PENDING.name()))
                    .andReturn();

            String response = result.getResponse().getContentAsString();
            createdTransactionRef = objectMapper.readTree(response).get("transactionReference").asText();
        }

        @Test
        @Order(2)
        @DisplayName("Should reject transfer with insufficient balance")
        @WithMockUser(username = "1", roles = {"REGULAR"})
        void testTransferInsufficientBalance() throws Exception {
            TransferRequest request = TransferRequest.builder()
                    .senderWalletNumber("12345678")
                    .receiverWalletNumber("87654321")
                    .amount(BigDecimal.valueOf(999999999))
                    .description("Test insufficient balance")
                    .build();

            mockMvc.perform(post("/api/transactions/transfer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("insufficient")));
        }

        @Test
        @DisplayName("Should reject transfer with invalid wallet number")
        @WithMockUser(username = "1", roles = {"REGULAR"})
        void testTransferInvalidWallet() throws Exception {
            TransferRequest request = TransferRequest.builder()
                    .senderWalletNumber("12345678")
                    .receiverWalletNumber("99999999")
                    .amount(BigDecimal.valueOf(100))
                    .description("Test invalid wallet")
                    .build();

            mockMvc.perform(post("/api/transactions/transfer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(containsString("Wallet not found")));
        }

        @Test
        @DisplayName("Should reject transfer without authentication")
        void testTransferUnauthorized() throws Exception {
            TransferRequest request = TransferRequest.builder()
                    .senderWalletNumber("12345678")
                    .receiverWalletNumber("87654321")
                    .amount(BigDecimal.valueOf(100))
                    .description("Test unauthorized")
                    .build();

            mockMvc.perform(post("/api/transactions/transfer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("LOT 1 - Deposit Tests")
    class DepositTests {

        @Test
        @DisplayName("Should create deposit successfully")
        @WithMockUser(username = "1", roles = {"REGULAR"})
        void testCreateDeposit() throws Exception {
            DepositRequest request = DepositRequest.builder()
                    .walletNumber("12345678")
                    .amount(BigDecimal.valueOf(5000))
                    .description("Test deposit")
                    .build();

            mockMvc.perform(post("/api/transactions/deposit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.transactionReference").exists())
                    .andExpect(jsonPath("$.type").value(TransactionType.DEPOSIT.name()))
                    .andExpect(jsonPath("$.amount").value(5000))
                    .andExpect(jsonPath("$.status").value(TransactionStatus.PENDING.name()));
        }

        @Test
        @DisplayName("Should reject deposit with negative amount")
        @WithMockUser(username = "1", roles = {"REGULAR"})
        void testDepositNegativeAmount() throws Exception {
            DepositRequest request = DepositRequest.builder()
                    .walletNumber("12345678")
                    .amount(BigDecimal.valueOf(-100))
                    .description("Test negative deposit")
                    .build();

            mockMvc.perform(post("/api/transactions/deposit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Merchant should be able to create deposit")
        @WithMockUser(username = "2", roles = {"MERCHANT"})
        void testMerchantDeposit() throws Exception {
            DepositRequest request = DepositRequest.builder()
                    .walletNumber("87654321")
                    .amount(BigDecimal.valueOf(10000))
                    .description("Merchant deposit")
                    .build();

            mockMvc.perform(post("/api/transactions/deposit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.type").value(TransactionType.DEPOSIT.name()));
        }
    }

    @Nested
    @DisplayName("LOT 1 - Withdrawal Tests")
    class WithdrawalTests {

        @Test
        @DisplayName("Should create withdrawal successfully")
        @WithMockUser(username = "1", roles = {"REGULAR"})
        void testCreateWithdrawal() throws Exception {
            WithdrawalRequest request = WithdrawalRequest.builder()
                    .walletNumber("12345678")
                    .amount(BigDecimal.valueOf(500))
                    .description("Test withdrawal")
                    .build();

            mockMvc.perform(post("/api/transactions/withdrawal")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.transactionReference").exists())
                    .andExpect(jsonPath("$.type").value(TransactionType.WITHDRAWAL.name()))
                    .andExpect(jsonPath("$.amount").value(500));
        }

        @Test
        @DisplayName("Should reject withdrawal exceeding balance")
        @WithMockUser(username = "1", roles = {"REGULAR"})
        void testWithdrawalExceedsBalance() throws Exception {
            WithdrawalRequest request = WithdrawalRequest.builder()
                    .walletNumber("12345678")
                    .amount(BigDecimal.valueOf(999999))
                    .description("Test exceeds balance")
                    .build();

            mockMvc.perform(post("/api/transactions/withdrawal")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("insufficient")));
        }
    }

    @Nested
    @DisplayName("LOT 1 - Merchant Payment Tests")
    class MerchantPaymentTests {

        @Test
        @DisplayName("Regular user should create merchant payment")
        @WithMockUser(username = "1", roles = {"REGULAR"})
        void testCreateMerchantPayment() throws Exception {
            PaymentRequest request = PaymentRequest.builder()
                    .payerWalletNumber("12345678")
                    .merchantWalletNumber("87654321")
                    .amount(BigDecimal.valueOf(250))
                    .description("Test merchant payment")
                    .build();

            mockMvc.perform(post("/api/transactions/merchant-payment")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.transactionReference").exists())
                    .andExpect(jsonPath("$.type").value(TransactionType.MERCHANT_PAYMENT.name()))
                    .andExpect(jsonPath("$.amount").value(250));
        }

        @Test
        @DisplayName("Merchant should NOT be able to pay themselves")
        @WithMockUser(username = "2", roles = {"MERCHANT"})
        void testMerchantCannotPaySelf() throws Exception {
            PaymentRequest request = PaymentRequest.builder()
                    .payerWalletNumber("87654321")
                    .merchantWalletNumber("87654321")
                    .amount(BigDecimal.valueOf(100))
                    .description("Self payment attempt")
                    .build();

            mockMvc.perform(post("/api/transactions/merchant-payment")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("cannot pay yourself")));
        }
    }

    @Nested
    @DisplayName("LOT 1 - Transaction Query Tests")
    class QueryTests {

        @Test
        @Order(3)
        @DisplayName("Should get transaction by reference")
        @WithMockUser(username = "1", roles = {"REGULAR"})
        void testGetTransactionByReference() throws Exception {
            // Use transaction created in transfer test
            if (createdTransactionRef != null) {
                mockMvc.perform(get("/api/transactions/" + createdTransactionRef))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.transactionReference").value(createdTransactionRef))
                        .andExpect(jsonPath("$.type").exists())
                        .andExpect(jsonPath("$.amount").exists())
                        .andExpect(jsonPath("$.status").exists());
            }
        }

        @Test
        @DisplayName("Should get wallet history")
        @WithMockUser(username = "1", roles = {"REGULAR"})
        void testGetWalletHistory() throws Exception {
            mockMvc.perform(get("/api/transactions/wallet/12345678")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.totalElements").exists())
                    .andExpect(jsonPath("$.totalPages").exists());
        }

        @Test
        @DisplayName("Admin should access transaction by ID")
        @WithMockUser(username = "3", roles = {"ADMIN"})
        void testAdminGetTransactionById() throws Exception {
            mockMvc.perform(get("/api/transactions/id/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));
        }

        @Test
        @DisplayName("Regular user should NOT access transaction by ID")
        @WithMockUser(username = "1", roles = {"REGULAR"})
        void testRegularUserCannotGetById() throws Exception {
            mockMvc.perform(get("/api/transactions/id/1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should NOT view other user's transaction")
        @WithMockUser(username = "999", roles = {"REGULAR"})
        void testCannotViewOthersTransaction() throws Exception {
            if (createdTransactionRef != null) {
                mockMvc.perform(get("/api/transactions/" + createdTransactionRef))
                        .andExpect(status().isForbidden());
            }
        }
    }

    @Nested
    @DisplayName("LOT 1 - Transaction Actions Tests")
    class ActionTests {

        @Test
        @DisplayName("Owner should cancel their transaction")
        @WithMockUser(username = "1", roles = {"REGULAR"})
        void testCancelOwnTransaction() throws Exception {
            if (createdTransactionRef != null) {
                mockMvc.perform(post("/api/transactions/" + createdTransactionRef + "/cancel")
                                .param("reason", "User requested cancellation"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value(TransactionStatus.CANCELLED.name()));
            }
        }

        @Test
        @DisplayName("Should NOT cancel other user's transaction")
        @WithMockUser(username = "999", roles = {"REGULAR"})
        void testCannotCancelOthersTransaction() throws Exception {
            if (createdTransactionRef != null) {
                mockMvc.perform(post("/api/transactions/" + createdTransactionRef + "/cancel")
                                .param("reason", "Unauthorized attempt"))
                        .andExpect(status().isForbidden());
            }
        }

        @Test
        @DisplayName("Should process pending transaction")
        @WithMockUser(username = "1", roles = {"REGULAR"})
        void testProcessTransaction() throws Exception {
            // Create a new transaction first
            TransferRequest request = TransferRequest.builder()
                    .senderWalletNumber("12345678")
                    .receiverWalletNumber("87654321")
                    .amount(BigDecimal.valueOf(100))
                    .description("Test process")
                    .build();

            MvcResult createResult = mockMvc.perform(post("/api/transactions/transfer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andReturn();

            String response = createResult.getResponse().getContentAsString();
            String txRef = objectMapper.readTree(response).get("transactionReference").asText();

            // Process it
            mockMvc.perform(post("/api/transactions/" + txRef + "/process"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(anyOf(
                            is(TransactionStatus.COMPLETED.name()),
                            is(TransactionStatus.PROCESSING.name())
                    )));
        }
    }

    @Nested
    @DisplayName("LOT 1 - Security Tests")
    class SecurityTests {

        @Test
        @DisplayName("All transaction endpoints should require authentication")
        void testEndpointsRequireAuth() throws Exception {
            mockMvc.perform(get("/api/transactions/wallet/12345678"))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(post("/api/transactions/transfer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(post("/api/transactions/deposit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Admin-only endpoints should reject regular users")
        @WithMockUser(username = "1", roles = {"REGULAR"})
        void testAdminEndpointsRejectRegular() throws Exception {
            mockMvc.perform(get("/api/transactions/id/1"))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/api/transactions/search")
                            .param("status", "PENDING"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Merchant role should have same access as regular for basic transactions")
        @WithMockUser(username = "2", roles = {"MERCHANT"})
        void testMerchantBasicAccess() throws Exception {
            mockMvc.perform(get("/api/transactions/wallet/87654321"))
                    .andExpect(status().isOk());

            TransferRequest request = TransferRequest.builder()
                    .senderWalletNumber("87654321")
                    .receiverWalletNumber("12345678")
                    .amount(BigDecimal.valueOf(100))
                    .description("Merchant transfer")
                    .build();

            mockMvc.perform(post("/api/transactions/transfer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("LOT 1 - Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should reject empty transfer request")
        @WithMockUser(username = "1", roles = {"REGULAR"})
        void testEmptyTransferRequest() throws Exception {
            mockMvc.perform(post("/api/transactions/transfer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject zero amount transfer")
        @WithMockUser(username = "1", roles = {"REGULAR"})
        void testZeroAmountTransfer() throws Exception {
            TransferRequest request = TransferRequest.builder()
                    .senderWalletNumber("12345678")
                    .receiverWalletNumber("87654321")
                    .amount(BigDecimal.ZERO)
                    .description("Zero amount")
                    .build();

            mockMvc.perform(post("/api/transactions/transfer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("amount")));
        }

        @Test
        @DisplayName("Should reject transfer to same wallet")
        @WithMockUser(username = "1", roles = {"REGULAR"})
        void testTransferToSameWallet() throws Exception {
            TransferRequest request = TransferRequest.builder()
                    .senderWalletNumber("12345678")
                    .receiverWalletNumber("12345678")
                    .amount(BigDecimal.valueOf(100))
                    .description("Self transfer")
                    .build();

            mockMvc.perform(post("/api/transactions/transfer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("same wallet")));
        }
    }
}
