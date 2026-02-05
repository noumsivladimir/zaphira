package com.zaphira.wallet.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaphira.wallet.dto.request.CreateWalletRequest;
import com.zaphira.wallet.dto.request.BalanceOperationRequest;
import com.zaphira.wallet.model.enums.WalletStatus;
import com.zaphira.wallet.model.enums.WalletType;
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
 * Tests d'intégration pour Wallet Service - LOT 1
 * Valide la gestion des wallets et opérations de base
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Wallet Service Integration Tests - LOT 1")
class WalletServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String createdWalletNumber;
    private static Long createdWalletId;

    @Nested
    @DisplayName("LOT 1 - Wallet Creation Tests")
    class WalletCreationTests {

        @Test
        @Order(1)
        @DisplayName("Regular user should create wallet successfully")
        @WithMockUser(username = "1", roles = {"REGULAR"})
        void testCreateRegularWallet() throws Exception {
            CreateWalletRequest request = CreateWalletRequest.builder()
                    .userId(1L)
                    .type(WalletType.USER)
                    .build();

            MvcResult result = mockMvc.perform(post("/api/wallets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.walletNumber").exists())
                    .andExpect(jsonPath("$.type").value(WalletType.USER.name()))
                    .andExpect(jsonPath("$.status").value(WalletStatus.ACTIVE.name()))
                    .andExpect(jsonPath("$.availableBalance").value(0))
                    .andExpect(jsonPath("$.blockedBalance").value(0))
                    .andExpect(jsonPath("$.totalBalance").value(0))
                    .andReturn();

            String response = result.getResponse().getContentAsString();
            createdWalletNumber = objectMapper.readTree(response).get("walletNumber").asText();
            createdWalletId = objectMapper.readTree(response).get("id").asLong();
        }

        @Test
        @DisplayName("Merchant should create merchant wallet")
        @WithMockUser(username = "2", roles = {"MERCHANT"})
        void testCreateMerchantWallet() throws Exception {
            CreateWalletRequest request = CreateWalletRequest.builder()
                    .userId(2L)
                    .type(WalletType.MERCHANT)
                    .build();

            mockMvc.perform(post("/api/wallets/merchant")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.walletNumber").exists())
                    .andExpect(jsonPath("$.type").value(WalletType.MERCHANT.name()));
        }

        @Test
        @DisplayName("Admin should create any type of wallet")
        @WithMockUser(username = "3", roles = {"ADMIN"})
        void testAdminCreateWallet() throws Exception {
            CreateWalletRequest request = CreateWalletRequest.builder()
                    .userId(999L)
                    .type(WalletType.USER)
                    .build();

            mockMvc.perform(post("/api/wallets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.walletNumber").exists());
        }

        @Test
        @DisplayName("Should prevent duplicate wallet for same user")
        @WithMockUser(username = "1", roles = {"REGULAR"})
        void testPreventDuplicateWallet() throws Exception {
            CreateWalletRequest request = CreateWalletRequest.builder()
                    .userId(1L)
                    .type(WalletType.USER)
                    .build();

            mockMvc.perform(post("/api/wallets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(containsString("already exists")));
        }

        @Test
        @DisplayName("Should generate unique wallet number")
        @WithMockUser(username = "4", roles = {"REGULAR"})
        void testUniqueWalletNumber() throws Exception {
            CreateWalletRequest request1 = CreateWalletRequest.builder()
                    .userId(4L)
                    .type(WalletType.USER)
                    .build();

            CreateWalletRequest request2 = CreateWalletRequest.builder()
                    .userId(5L)
                    .type(WalletType.USER)
                    .build();

            MvcResult result1 = mockMvc.perform(post("/api/wallets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request1)))
                    .andExpect(status().isCreated())
                    .andReturn();

            MvcResult result2 = mockMvc.perform(post("/api/wallets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request2)))
                    .andExpect(status().isCreated())
                    .andReturn();

            String walletNumber1 = objectMapper.readTree(result1.getResponse().getContentAsString())
                    .get("walletNumber").asText();
            String walletNumber2 = objectMapper.readTree(result2.getResponse().getContentAsString())
                    .get("walletNumber").asText();

            assert !walletNumber1.equals(walletNumber2);
        }
    }

    @Nested
    @DisplayName("LOT 1 - Wallet Query Tests")
    class WalletQueryTests {

        @Test
        @Order(2)
        @DisplayName("Owner should view their wallet")
        @WithMockUser(username = "1", roles = {"REGULAR"})
        void testGetOwnWallet() throws Exception {
            if (createdWalletNumber != null) {
                mockMvc.perform(get("/api/wallets/" + createdWalletNumber))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.walletNumber").value(createdWalletNumber))
                        .andExpect(jsonPath("$.userId").value(1))
                        .andExpect(jsonPath("$.type").exists())
                        .andExpect(jsonPath("$.status").exists())
                        .andExpect(jsonPath("$.availableBalance").exists());
            }
        }

        @Test
        @DisplayName("Should NOT view other user's wallet")
        @WithMockUser(username = "999", roles = {"REGULAR"})
        void testCannotViewOthersWallet() throws Exception {
            if (createdWalletNumber != null) {
                mockMvc.perform(get("/api/wallets/" + createdWalletNumber))
                        .andExpect(status().isForbidden());
            }
        }

        @Test
        @DisplayName("Admin should view any wallet")
        @WithMockUser(username = "3", roles = {"ADMIN"})
        void testAdminViewAnyWallet() throws Exception {
            if (createdWalletNumber != null) {
                mockMvc.perform(get("/api/wallets/" + createdWalletNumber))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.walletNumber").value(createdWalletNumber));
            }
        }

        @Test
        @DisplayName("Admin should get wallet by ID")
        @WithMockUser(username = "3", roles = {"ADMIN"})
        void testGetWalletById() throws Exception {
            if (createdWalletId != null) {
                mockMvc.perform(get("/api/wallets/id/" + createdWalletId))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id").value(createdWalletId));
            }
        }

        @Test
        @DisplayName("Regular user should NOT get wallet by ID")
        @WithMockUser(username = "1", roles = {"REGULAR"})
        void testRegularCannotGetById() throws Exception {
            if (createdWalletId != null) {
                mockMvc.perform(get("/api/wallets/id/" + createdWalletId))
                        .andExpect(status().isForbidden());
            }
        }

        @Test
        @DisplayName("Should get wallet summary for user")
        @WithMockUser(username = "1", roles = {"REGULAR"})
        void testGetWalletSummary() throws Exception {
            mockMvc.perform(get("/api/wallets/user/1/summary"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(1))
                    .andExpect(jsonPath("$.totalWallets").exists())
                    .andExpect(jsonPath("$.totalBalanceAllWallets").exists());
        }

        @Test
        @DisplayName("Should NOT get other user's summary")
        @WithMockUser(username = "1", roles = {"REGULAR"})
        void testCannotGetOthersSummary() throws Exception {
            mockMvc.perform(get("/api/wallets/user/999/summary"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Admin should get any user's summary")
        @WithMockUser(username = "3", roles = {"ADMIN"})
        void testAdminGetAnySummary() throws Exception {
            mockMvc.perform(get("/api/wallets/user/1/summary"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(1));
        }
    }

    @Nested
    @DisplayName("LOT 1 - Balance Operations Tests")
    class BalanceOperationsTests {

        @Test
        @DisplayName("Admin should credit wallet")
        @WithMockUser(username = "3", roles = {"ADMIN"})
        void testCreditWallet() throws Exception {
            if (createdWalletId != null) {
                BalanceOperationRequest request = BalanceOperationRequest.builder()
                        .amount(BigDecimal.valueOf(1000))
                        .description("Test credit")
                        .build();

                mockMvc.perform(post("/api/wallets/" + createdWalletId + "/credit")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.availableBalance").value(greaterThanOrEqualTo(1000.0)));
            }
        }

        @Test
        @DisplayName("Regular user should NOT credit wallet")
        @WithMockUser(username = "1", roles = {"REGULAR"})
        void testRegularCannotCredit() throws Exception {
            if (createdWalletId != null) {
                BalanceOperationRequest request = BalanceOperationRequest.builder()
                        .amount(BigDecimal.valueOf(1000))
                        .description("Unauthorized credit")
                        .build();

                mockMvc.perform(post("/api/wallets/" + createdWalletId + "/credit")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isForbidden());
            }
        }

        @Test
        @DisplayName("Admin should debit wallet")
        @WithMockUser(username = "3", roles = {"ADMIN"})
        void testDebitWallet() throws Exception {
            if (createdWalletId != null) {
                // First credit to have balance
                BalanceOperationRequest creditRequest = BalanceOperationRequest.builder()
                        .amount(BigDecimal.valueOf(1000))
                        .description("Setup balance")
                        .build();

                mockMvc.perform(post("/api/wallets/" + createdWalletId + "/credit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creditRequest)));

                // Then debit
                BalanceOperationRequest debitRequest = BalanceOperationRequest.builder()
                        .amount(BigDecimal.valueOf(500))
                        .description("Test debit")
                        .build();

                mockMvc.perform(post("/api/wallets/" + createdWalletId + "/debit")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(debitRequest)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.availableBalance").value(lessThanOrEqualTo(500.0)));
            }
        }

        @Test
        @DisplayName("Should reject debit exceeding balance")
        @WithMockUser(username = "3", roles = {"ADMIN"})
        void testDebitExceedsBalance() throws Exception {
            if (createdWalletId != null) {
                BalanceOperationRequest request = BalanceOperationRequest.builder()
                        .amount(BigDecimal.valueOf(999999))
                        .description("Excessive debit")
                        .build();

                mockMvc.perform(post("/api/wallets/" + createdWalletId + "/debit")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.message").value(containsString("insufficient")));
            }
        }

        @Test
        @DisplayName("Should block amount successfully")
        @WithMockUser(username = "3", roles = {"ADMIN"})
        void testBlockAmount() throws Exception {
            if (createdWalletId != null) {
                // Ensure balance
                BalanceOperationRequest creditRequest = BalanceOperationRequest.builder()
                        .amount(BigDecimal.valueOf(1000))
                        .description("Setup balance")
                        .build();

                mockMvc.perform(post("/api/wallets/" + createdWalletId + "/credit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creditRequest)));

                // Block amount
                mockMvc.perform(post("/api/wallets/" + createdWalletId + "/block")
                                .param("amount", "200")
                                .param("reason", "Test block"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.blockedBalance").value(greaterThanOrEqualTo(200.0)));
            }
        }

        @Test
        @DisplayName("Should unblock amount successfully")
        @WithMockUser(username = "3", roles = {"ADMIN"})
        void testUnblockAmount() throws Exception {
            if (createdWalletId != null) {
                // Block first
                mockMvc.perform(post("/api/wallets/" + createdWalletId + "/block")
                        .param("amount", "100")
                        .param("reason", "Setup"));

                // Then unblock
                mockMvc.perform(post("/api/wallets/" + createdWalletId + "/unblock")
                                .param("amount", "100")
                                .param("reason", "Test unblock"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.blockedBalance").value(lessThanOrEqualTo(100.0)));
            }
        }
    }

    @Nested
    @DisplayName("LOT 1 - Wallet Lifecycle Tests")
    class LifecycleTests {

        @Test
        @DisplayName("Admin should freeze wallet")
        @WithMockUser(username = "3", roles = {"ADMIN"})
        void testFreezeWallet() throws Exception {
            if (createdWalletNumber != null) {
                mockMvc.perform(put("/api/wallets/" + createdWalletNumber + "/freeze")
                                .param("reason", "Suspicious activity"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value(WalletStatus.FROZEN.name()))
                        .andExpect(jsonPath("$.frozenReason").value("Suspicious activity"));
            }
        }

        @Test
        @DisplayName("Regular user should NOT freeze wallet")
        @WithMockUser(username = "1", roles = {"REGULAR"})
        void testRegularCannotFreeze() throws Exception {
            if (createdWalletNumber != null) {
                mockMvc.perform(put("/api/wallets/" + createdWalletNumber + "/freeze")
                                .param("reason", "Unauthorized"))
                        .andExpect(status().isForbidden());
            }
        }

        @Test
        @DisplayName("Admin should unfreeze wallet")
        @WithMockUser(username = "3", roles = {"ADMIN"})
        void testUnfreezeWallet() throws Exception {
            if (createdWalletNumber != null) {
                // Freeze first
                mockMvc.perform(put("/api/wallets/" + createdWalletNumber + "/freeze")
                        .param("reason", "Test"));

                // Then unfreeze
                mockMvc.perform(put("/api/wallets/" + createdWalletNumber + "/unfreeze"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value(WalletStatus.ACTIVE.name()));
            }
        }

        @Test
        @DisplayName("Admin should suspend wallet")
        @WithMockUser(username = "3", roles = {"ADMIN"})
        void testSuspendWallet() throws Exception {
            if (createdWalletNumber != null) {
                mockMvc.perform(put("/api/wallets/" + createdWalletNumber + "/suspend")
                                .param("reason", "Policy violation"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value(WalletStatus.SUSPENDED.name()));
            }
        }

        @Test
        @DisplayName("Admin should activate wallet")
        @WithMockUser(username = "3", roles = {"ADMIN"})
        void testActivateWallet() throws Exception {
            if (createdWalletNumber != null) {
                mockMvc.perform(put("/api/wallets/" + createdWalletNumber + "/activate"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value(WalletStatus.ACTIVE.name()));
            }
        }

        @Test
        @DisplayName("Admin should close wallet")
        @WithMockUser(username = "3", roles = {"ADMIN"})
        void testCloseWallet() throws Exception {
            // Create new wallet for closing
            CreateWalletRequest createRequest = CreateWalletRequest.builder()
                    .userId(100L)
                    .type(WalletType.USER)
                    .build();

            MvcResult createResult = mockMvc.perform(post("/api/wallets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isCreated())
                    .andReturn();

            String walletNumber = objectMapper.readTree(createResult.getResponse().getContentAsString())
                    .get("walletNumber").asText();

            // Close it
            mockMvc.perform(put("/api/wallets/" + walletNumber + "/close")
                            .param("reason", "User request"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(WalletStatus.CLOSED.name()));
        }
    }

    @Nested
    @DisplayName("LOT 1 - Security Tests")
    class SecurityTests {

        @Test
        @DisplayName("All endpoints should require authentication")
        void testEndpointsRequireAuth() throws Exception {
            mockMvc.perform(get("/api/wallets/12345678"))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(post("/api/wallets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Admin-only operations should reject non-admin")
        @WithMockUser(username = "1", roles = {"REGULAR"})
        void testAdminOnlyOperations() throws Exception {
            if (createdWalletNumber != null) {
                mockMvc.perform(put("/api/wallets/" + createdWalletNumber + "/freeze")
                                .param("reason", "Test"))
                        .andExpect(status().isForbidden());

                mockMvc.perform(put("/api/wallets/" + createdWalletNumber + "/close")
                                .param("reason", "Test"))
                        .andExpect(status().isForbidden());
            }
        }
    }

    @Nested
    @DisplayName("LOT 1 - Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should reject empty create wallet request")
        @WithMockUser(username = "1", roles = {"REGULAR"})
        void testEmptyCreateRequest() throws Exception {
            mockMvc.perform(post("/api/wallets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject negative credit amount")
        @WithMockUser(username = "3", roles = {"ADMIN"})
        void testNegativeCreditAmount() throws Exception {
            if (createdWalletId != null) {
                BalanceOperationRequest request = BalanceOperationRequest.builder()
                        .amount(BigDecimal.valueOf(-100))
                        .description("Negative credit")
                        .build();

                mockMvc.perform(post("/api/wallets/" + createdWalletId + "/credit")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());
            }
        }

        @Test
        @DisplayName("Should reject zero amount credit")
        @WithMockUser(username = "3", roles = {"ADMIN"})
        void testZeroAmountCredit() throws Exception {
            if (createdWalletId != null) {
                BalanceOperationRequest request = BalanceOperationRequest.builder()
                        .amount(BigDecimal.ZERO)
                        .description("Zero credit")
                        .build();

                mockMvc.perform(post("/api/wallets/" + createdWalletId + "/credit")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());
            }
        }
    }
}
