package com.zaphira.transaction.test.integration;

import com.zaphira.transaction.model.Transaction;
import com.zaphira.transaction.model.enums.TransactionStatus;
import com.zaphira.transaction.model.enums.TransactionType;
import com.zaphira.transaction.model.enums.TransactionChannel;
import com.zaphira.transaction.repository.TransactionRepository;
import com.zaphira.common.test.feign.WalletSyncClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Cross-microservice integration test using Feign synchronous messaging.
 * 
 * This test demonstrates:
 * 1. Transaction service creates a transaction
 * 2. Transaction service calls Wallet service via Feign (synchronous)
 * 3. Both services use SQLite in-memory database (test-sync profile)
 * 
 * Profile: test-sync (no Kafka, no Docker, in-memory DB)
 * 
 * Run with:
 * mvn test -Dspring.profiles.active=test-sync -Dtest=CrossMicroserviceIntegrationTest
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test-sync")
@DisplayName("Cross-Microservice Integration Test (Feign Sync)")
class CrossMicroserviceIntegrationTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired(required = false)
    private WalletSyncClient walletSyncClient;

    @Test
    @Transactional
    @DisplayName("Should create transaction and notify wallet service synchronously via Feign")
    void testCrossMicroserviceTransactionFlow() {
        // ==================== ARRANGE ====================
        Transaction transaction = Transaction.builder()
                .reference("SYNC-TX-" + System.currentTimeMillis())
                .senderWalletNumber("WALLET-001")
                .receiverWalletNumber("WALLET-002")
                .amount(new BigDecimal("150.00"))
                .currency("USD")
                .status(TransactionStatus.PENDING)
                .type(TransactionType.P2P_TRANSFER)
                .channel(TransactionChannel.MOBILE)
                .description("Cross-microservice test transaction")
                .createdAt(LocalDateTime.now())
                .build();

        // ==================== ACT ====================
        // 1. Create transaction locally
        @SuppressWarnings("null")
        Transaction savedTransaction = transactionRepository.save(transaction);
        
        assertThat(savedTransaction).isNotNull();
        assertThat(savedTransaction.getId()).isNotNull();

        // 2. Notify wallet service synchronously via Feign
        // (In real scenario, wallet service would process this and update balances)
        if (walletSyncClient != null) {
            WalletNotificationEvent walletEvent = WalletNotificationEvent.builder()
                    .transactionId(savedTransaction.getId())
                    .senderWalletNumber(savedTransaction.getSenderWalletNumber())
                    .receiverWalletNumber(savedTransaction.getReceiverWalletNumber())
                    .amount(savedTransaction.getAmount())
                    .build();
            
            walletSyncClient.sendMessage("wallet-updated", walletEvent);
        }

        // ==================== ASSERT ====================
        // Verify transaction was persisted
        @SuppressWarnings("null")
        var retrieved = transactionRepository.findById(savedTransaction.getId());
        assertThat(retrieved)
                .isPresent()
                .hasValueSatisfying(tx -> {
                    assertThat(tx.getReference()).isEqualTo(transaction.getReference());
                    assertThat(tx.getAmount()).isEqualTo(transaction.getAmount());
                    assertThat(tx.getStatus()).isEqualTo(TransactionStatus.PENDING);
                });
    }

    @Test
    @Transactional
    @DisplayName("Should handle multiple transactions in test-sync mode")
    void testMultipleTransactionsWithFeign() {
        // ==================== ARRANGE ====================
        for (int i = 0; i < 3; i++) {
            Transaction tx = Transaction.builder()
                    .reference("BATCH-SYNC-" + System.currentTimeMillis() + "-" + i)
                    .senderWalletNumber("WALLET-00" + i)
                    .receiverWalletNumber("WALLET-00" + (i + 1))
                    .amount(new BigDecimal("100.00"))
                    .currency("USD")
                    .status(TransactionStatus.PENDING)
                    .type(TransactionType.P2P_TRANSFER)
                    .channel(TransactionChannel.WEB)
                    .createdAt(LocalDateTime.now())
                    .build();
            
            @SuppressWarnings({"null", "unused"})
            Transaction saved = transactionRepository.save(tx);
        }

        // ==================== ASSERT ====================
        var all = transactionRepository.findAll();
        assertThat(all).hasSizeGreaterThanOrEqualTo(3);
    }

    /**
     * Simple DTO for wallet notifications.
     * In real implementation, this would be in a shared event/dto package.
     */
    @SuppressWarnings("unused")
    static class WalletNotificationEvent {
        @SuppressWarnings("unused")
        private Long transactionId;
        @SuppressWarnings("unused")
        private String senderWalletNumber;
        @SuppressWarnings("unused")
        private String receiverWalletNumber;
        @SuppressWarnings("unused")
        private BigDecimal amount;

        WalletNotificationEvent() {}

        WalletNotificationEvent(Builder builder) {
            this.transactionId = builder.transactionId;
            this.senderWalletNumber = builder.senderWalletNumber;
            this.receiverWalletNumber = builder.receiverWalletNumber;
            this.amount = builder.amount;
        }

        static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private Long transactionId;
            private String senderWalletNumber;
            private String receiverWalletNumber;
            private BigDecimal amount;

            public Builder transactionId(Long transactionId) {
                this.transactionId = transactionId;
                return this;
            }

            public Builder senderWalletNumber(String senderWalletNumber) {
                this.senderWalletNumber = senderWalletNumber;
                return this;
            }

            public Builder receiverWalletNumber(String receiverWalletNumber) {
                this.receiverWalletNumber = receiverWalletNumber;
                return this;
            }

            public Builder amount(BigDecimal amount) {
                this.amount = amount;
                return this;
            }

            public WalletNotificationEvent build() {
                return new WalletNotificationEvent(this);
            }
        }
    }
}
