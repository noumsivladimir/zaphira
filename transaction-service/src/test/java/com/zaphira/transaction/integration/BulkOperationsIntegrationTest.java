package com.zaphira.transaction.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaphira.transaction.dto.requests.BulkTransferRequest;
import com.zaphira.transaction.dto.requests.BulkTransferItemRequest;
import com.zaphira.transaction.dto.requests.SplitPaymentRequest;
import com.zaphira.transaction.dto.requests.TransferRequest;
import com.zaphira.transaction.model.enums.TransactionCategory;
import com.zaphira.transaction.model.enums.TransactionChannel;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Tests d'intégration pour Transaction Service - LOT 2
 * Valide les paiements bulk et split payments
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Transaction Service Integration Tests - LOT 2")
class BulkOperationsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("LOT 2 - Bulk Transfer Tests")
    class BulkTransferTests {

        @Test
        @DisplayName("Merchant should create bulk transfer successfully")
        @WithMockUser(username = "2", roles = {"MERCHANT"})
        void testMerchantBulkTransfer() throws Exception {
            BulkTransferRequest request = BulkTransferRequest.builder()
                    .senderWalletNumber("87654321")
                    .items(Arrays.asList(
                            BulkTransferItemRequest.builder()
                                    .receiverWalletNumber("12345678")
                                    .amount(BigDecimal.valueOf(500))
                                    .description("Bulk payment 1")
                                    .category(TransactionCategory.WALLET_TO_WALLET)
                                    .channel(TransactionChannel.WEB)
                                    .build(),
                            BulkTransferItemRequest.builder()
                                    .receiverWalletNumber("11111111")
                                    .amount(BigDecimal.valueOf(300))
                                    .description("Bulk payment 2")
                                    .category(TransactionCategory.WALLET_TO_WALLET)
                                    .channel(TransactionChannel.WEB)
                                    .build()
                    ))
                    .build();

            mockMvc.perform(post("/api/transactions/bulk-transfer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.batchId").exists())
                    .andExpect(jsonPath("$.totalAmount").value(800))
                    .andExpect(jsonPath("$.successCount").exists())
                    .andExpect(jsonPath("$.transactions").isArray())
                    .andExpect(jsonPath("$.transactions", hasSize(2)));
        }

        @Test
        @DisplayName("Admin should create bulk transfer")
        @WithMockUser(username = "3", roles = {"ADMIN"})
        void testAdminBulkTransfer() throws Exception {
            BulkTransferRequest request = BulkTransferRequest.builder()
                    .senderWalletNumber("87654321")
                    .items(Arrays.asList(
                            BulkTransferItemRequest.builder()
                                    .receiverWalletNumber("12345678")
                                    .amount(BigDecimal.valueOf(100))
                                    .description("Admin bulk 1")
                                    .category(TransactionCategory.WALLET_TO_WALLET)
                                    .channel(TransactionChannel.WEB)
                                    .build()
                    ))
                    .build();

            mockMvc.perform(post("/api/transactions/bulk-transfer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.batchId").exists());
        }

        @Test
        @DisplayName("Regular user should NOT create bulk transfer")
        @WithMockUser(username = "1", roles = {"REGULAR"})
        void testRegularUserCannotBulkTransfer() throws Exception {
            BulkTransferRequest request = BulkTransferRequest.builder()
                    .senderWalletNumber("12345678")
                    .items(Arrays.asList(
                            BulkTransferItemRequest.builder()
                                    .receiverWalletNumber("87654321")
                                    .amount(BigDecimal.valueOf(100))
                                    .description("Unauthorized bulk")
                                    .category(TransactionCategory.WALLET_TO_WALLET)
                                    .channel(TransactionChannel.WEB)
                                    .build()
                    ))
                    .build();

            mockMvc.perform(post("/api/transactions/bulk-transfer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should reject empty recipients list")
        @WithMockUser(username = "2", roles = {"MERCHANT"})
        void testEmptyRecipientsList() throws Exception {
            BulkTransferRequest request = BulkTransferRequest.builder()
                    .senderWalletNumber("87654321")
                    .items(Arrays.asList())
                    .build();

            mockMvc.perform(post("/api/transactions/bulk-transfer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("recipients")));
        }

        @Test
        @DisplayName("Should reject bulk transfer with insufficient balance")
        @WithMockUser(username = "2", roles = {"MERCHANT"})
        void testBulkTransferInsufficientBalance() throws Exception {
            BulkTransferRequest request = BulkTransferRequest.builder()
                    .senderWalletNumber("87654321")
                    .items(Arrays.asList(
                            BulkTransferItemRequest.builder()
                                    .receiverWalletNumber("12345678")
                                    .amount(BigDecimal.valueOf(999999999))
                                    .description("Too much")
                                    .category(TransactionCategory.WALLET_TO_WALLET)
                                    .channel(TransactionChannel.WEB)
                                    .build()
                    ))
                    .build();

            mockMvc.perform(post("/api/transactions/bulk-transfer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("insufficient")));
        }

        @Test
        @DisplayName("Bulk transfer should be atomic - all or nothing")
        @WithMockUser(username = "2", roles = {"MERCHANT"})
        void testBulkTransferAtomicity() throws Exception {
            BulkTransferRequest request = BulkTransferRequest.builder()
                    .senderWalletNumber("87654321")
                    .items(Arrays.asList(
                            BulkTransferItemRequest.builder()
                                    .receiverWalletNumber("12345678")
                                    .amount(BigDecimal.valueOf(100))
                                    .description("Valid recipient")
                                    .category(TransactionCategory.WALLET_TO_WALLET)
                                    .channel(TransactionChannel.WEB)
                                    .build(),
                            BulkTransferItemRequest.builder()
                                    .receiverWalletNumber("99999999") // Invalid wallet
                                    .amount(BigDecimal.valueOf(100))
                                    .description("Invalid recipient")
                                    .category(TransactionCategory.WALLET_TO_WALLET)
                                    .channel(TransactionChannel.WEB)
                                    .build()
                    ))
                    .build();

            mockMvc.perform(post("/api/transactions/bulk-transfer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            // Verify no transactions were created
            // Check wallet balance remained unchanged
        }
    }

    @Nested
    @DisplayName("LOT 2 - Split Payment Tests")
    class SplitPaymentTests {

        @Test
        @DisplayName("Should create split payment successfully")
        @WithMockUser(username = "1", roles = {"REGULAR"})
        void testCreateSplitPayment() throws Exception {
            SplitPaymentRequest request = SplitPaymentRequest.builder()
                    .senderWalletNumber("12345678")
                    .totalAmount(BigDecimal.valueOf(1000))
                    .description("Split restaurant bill")
                    .recipients(Arrays.asList(
                            SplitPaymentRequest.RecipientShare.builder()
                                    .recipientWalletNumber("87654321")
                                    .amount(BigDecimal.valueOf(600))
                                    .label("Main share")
                                    .build(),
                            SplitPaymentRequest.RecipientShare.builder()
                                    .recipientWalletNumber("11111111")
                                    .amount(BigDecimal.valueOf(400))
                                    .label("Secondary share")
                                    .build()
                    ))
                    .build();

            mockMvc.perform(post("/api/transactions/split-payment")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.batchId").exists())
                    .andExpect(jsonPath("$.batchId").value(startsWith("SPLIT-")))
                    .andExpect(jsonPath("$.totalAmount").value(1000))
                    .andExpect(jsonPath("$.recipientCount").value(2))
                    .andExpect(jsonPath("$.transactions").isArray())
                    .andExpect(jsonPath("$.transactions", hasSize(2)))
                    .andExpect(jsonPath("$.status").value("SUCCESS"));
        }

        @Test
        @DisplayName("Merchant should create split payment")
        @WithMockUser(username = "2", roles = {"MERCHANT"})
        void testMerchantSplitPayment() throws Exception {
            SplitPaymentRequest request = SplitPaymentRequest.builder()
                    .senderWalletNumber("87654321")
                    .totalAmount(BigDecimal.valueOf(500))
                    .description("Merchant split")
                    .recipients(Arrays.asList(
                            SplitPaymentRequest.RecipientShare.builder()
                                    .recipientWalletNumber("12345678")
                                    .amount(BigDecimal.valueOf(250))
                                    .label("Share 1")
                                    .build(),
                            SplitPaymentRequest.RecipientShare.builder()
                                    .recipientWalletNumber("11111111")
                                    .amount(BigDecimal.valueOf(250))
                                    .label("Share 2")
                                    .build()
                    ))
                    .build();

            mockMvc.perform(post("/api/transactions/split-payment")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("SUCCESS"));
        }

        @Test
        @DisplayName("Should reject split payment if shares don't sum to total")
        @WithMockUser(username = "1", roles = {"REGULAR"})
        void testSplitPaymentSharesMismatch() throws Exception {
            SplitPaymentRequest request = SplitPaymentRequest.builder()
                    .senderWalletNumber("12345678")
                    .totalAmount(BigDecimal.valueOf(1000))
                    .description("Mismatched split")
                    .recipients(Arrays.asList(
                            SplitPaymentRequest.RecipientShare.builder()
                                    .recipientWalletNumber("87654321")
                                    .amount(BigDecimal.valueOf(600))
                                    .label("Share 1")
                                    .build(),
                            SplitPaymentRequest.RecipientShare.builder()
                                    .recipientWalletNumber("11111111")
                                    .amount(BigDecimal.valueOf(300)) // Total = 900, not 1000
                                    .label("Share 2")
                                    .build()
                    ))
                    .build();

            mockMvc.perform(post("/api/transactions/split-payment")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("sum")));
        }

        @Test
        @DisplayName("Should reject split payment with less than 2 recipients")
        @WithMockUser(username = "1", roles = {"REGULAR"})
        void testSplitPaymentMinimumRecipients() throws Exception {
            SplitPaymentRequest request = SplitPaymentRequest.builder()
                    .senderWalletNumber("12345678")
                    .totalAmount(BigDecimal.valueOf(100))
                    .description("Single recipient")
                    .recipients(Arrays.asList(
                            SplitPaymentRequest.RecipientShare.builder()
                                    .recipientWalletNumber("87654321")
                                    .amount(BigDecimal.valueOf(100))
                                    .label("Only one")
                                    .build()
                    ))
                    .build();

            mockMvc.perform(post("/api/transactions/split-payment")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("at least 2 recipients")));
        }

        @Test
        @DisplayName("Split payment should be atomic")
        @WithMockUser(username = "1", roles = {"REGULAR"})
        void testSplitPaymentAtomicity() throws Exception {
            SplitPaymentRequest request = SplitPaymentRequest.builder()
                    .senderWalletNumber("12345678")
                    .totalAmount(BigDecimal.valueOf(500))
                    .description("Atomic test")
                    .recipients(Arrays.asList(
                            SplitPaymentRequest.RecipientShare.builder()
                                    .recipientWalletNumber("87654321")
                                    .amount(BigDecimal.valueOf(250))
                                    .label("Valid")
                                    .build(),
                            SplitPaymentRequest.RecipientShare.builder()
                                    .recipientWalletNumber("99999999") // Invalid
                                    .amount(BigDecimal.valueOf(250))
                                    .label("Invalid")
                                    .build()
                    ))
                    .build();

            mockMvc.perform(post("/api/transactions/split-payment")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());

            // Verify no partial transactions were created
        }

        @Test
        @DisplayName("Should reject split payment with insufficient balance")
        @WithMockUser(username = "1", roles = {"REGULAR"})
        void testSplitPaymentInsufficientBalance() throws Exception {
            SplitPaymentRequest request = SplitPaymentRequest.builder()
                    .senderWalletNumber("12345678")
                    .totalAmount(BigDecimal.valueOf(999999999))
                    .description("Too much")
                    .recipients(Arrays.asList(
                            SplitPaymentRequest.RecipientShare.builder()
                                    .recipientWalletNumber("87654321")
                                    .amount(BigDecimal.valueOf(499999999))
                                    .label("Share 1")
                                    .build(),
                            SplitPaymentRequest.RecipientShare.builder()
                                    .recipientWalletNumber("11111111")
                                    .amount(BigDecimal.valueOf(500000000))
                                    .label("Share 2")
                                    .build()
                    ))
                    .build();

            mockMvc.perform(post("/api/transactions/split-payment")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("insufficient")));
        }
    }

    @Nested
    @DisplayName("LOT 2 - Transaction Fees Tests")
    class FeesTests {

        @Test
        @DisplayName("Transactions should calculate fees correctly")
        @WithMockUser(username = "1", roles = {"REGULAR"})
        void testFeesCalculation() throws Exception {
            TransferRequest request = TransferRequest.builder()
                    .senderWalletNumber("12345678")
                    .receiverWalletNumber("87654321")
                    .amount(BigDecimal.valueOf(1000))
                    .description("Test with fees")
                    .build();

            mockMvc.perform(post("/api/transactions/transfer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.fees").exists())
                    .andExpect(jsonPath("$.fees.totalFee").exists())
                    .andExpect(jsonPath("$.fees.platformFee").exists());
        }

        @Test
        @DisplayName("Bulk transfer should calculate total fees")
        @WithMockUser(username = "2", roles = {"MERCHANT"})
        void testBulkFeesCalculation() throws Exception {
            BulkTransferRequest request = BulkTransferRequest.builder()
                    .senderWalletNumber("87654321")
                    .items(Arrays.asList(
                            BulkTransferItemRequest.builder()
                                    .receiverWalletNumber("12345678")
                                    .amount(BigDecimal.valueOf(500))
                                    .description("Recipient 1")
                                    .category(TransactionCategory.WALLET_TO_WALLET)
                                    .channel(TransactionChannel.WEB)
                                    .build(),
                            BulkTransferItemRequest.builder()
                                    .receiverWalletNumber("11111111")
                                    .amount(BigDecimal.valueOf(500))
                                    .description("Recipient 2")
                                    .category(TransactionCategory.WALLET_TO_WALLET)
                                    .channel(TransactionChannel.WEB)
                                    .build()
                    ))
                    .build();

            mockMvc.perform(post("/api/transactions/bulk-transfer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.totalFees").exists());
        }
    }

    @Nested
    @DisplayName("LOT 2 - Security Tests")
    class SecurityTests {

        @Test
        @DisplayName("Bulk transfer requires authentication")
        void testBulkTransferRequiresAuth() throws Exception {
            mockMvc.perform(post("/api/transactions/bulk-transfer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Split payment requires authentication")
        void testSplitPaymentRequiresAuth() throws Exception {
            mockMvc.perform(post("/api/transactions/split-payment")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Regular user cannot access bulk transfer")
        @WithMockUser(username = "1", roles = {"REGULAR"})
        void testRegularUserBulkAccess() throws Exception {
            BulkTransferRequest request = BulkTransferRequest.builder()
                    .senderWalletNumber("12345678")
                    .items(Arrays.asList())
                    .build();

            mockMvc.perform(post("/api/transactions/bulk-transfer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }
}
