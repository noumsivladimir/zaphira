package com.zaphira.transaction.integration;

import com.zaphira.transaction.model.Transaction;
import com.zaphira.transaction.model.enums.TransactionStatus;
import com.zaphira.transaction.model.enums.TransactionType;
import com.zaphira.transaction.model.enums.TransactionChannel;
import com.zaphira.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * End-to-end integration test for Transaction service using SQLite.
 * 
 * This test uses @SpringBootTest to load the full application context,
 * allowing testing of transactional behavior and integration between layers.
 * 
 * Profile: "test" activates SQLite in-memory database via application-test.yml
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Transaction Service - Full Integration Tests with SQLite")
class TransactionServiceIntegrationTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
    }

    @Test
    @Transactional
    @DisplayName("Should create and retrieve transaction in a single transaction")
    void testTransactionalBehavior() {
        // Arrange
        Transaction transaction = Transaction.builder()
                .reference("TXN-TX-001")
                .senderWalletNumber("WALLET-001")
                .receiverWalletNumber("WALLET-002")
                .amount(new BigDecimal("250.50"))
                .currency("USD")
                .status(TransactionStatus.PENDING)
                .type(TransactionType.P2P_TRANSFER)
                .channel(TransactionChannel.MOBILE)
                .description("Transactional test")
                .createdAt(LocalDateTime.now())
                .build();

        // Act
        @SuppressWarnings("null")
        Transaction saved = transactionRepository.save(transaction);

        // Assert
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(transactionRepository.count()).isEqualTo(1);
    }

    @Test
    @Transactional
    @DisplayName("Should handle multiple transactions in sequence")
    void testMultipleTransactions() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        
        Transaction tx1 = Transaction.builder()
                .reference("BULK-TX-001")
                .senderWalletNumber("WALLET-001")
                .receiverWalletNumber("WALLET-002")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .status(TransactionStatus.PENDING)
                .type(TransactionType.P2P_TRANSFER)
                .channel(TransactionChannel.WEB)
                .createdAt(now)
                .build();

        Transaction tx2 = Transaction.builder()
                .reference("BULK-TX-002")
                .senderWalletNumber("WALLET-002")
                .receiverWalletNumber("WALLET-003")
                .amount(new BigDecimal("150.00"))
                .currency("EUR")
                .status(TransactionStatus.COMPLETED)
                .type(TransactionType.P2P_TRANSFER)
                .channel(TransactionChannel.API)
                .createdAt(now.plusMinutes(5))
                .build();

        Transaction tx3 = Transaction.builder()
                .reference("BULK-TX-003")
                .senderWalletNumber("WALLET-003")
                .receiverWalletNumber("WALLET-001")
                .amount(new BigDecimal("200.00"))
                .currency("USD")
                .status(TransactionStatus.FAILED)
                .type(TransactionType.P2P_TRANSFER)
                .channel(TransactionChannel.MOBILE)
                .createdAt(now.plusMinutes(10))
                .build();

        // Act
       
        @SuppressWarnings("unused")
        var result = transactionRepository.saveAll(List.of(tx1, tx2, tx3));

        // Assert
        assertThat(transactionRepository.count()).isEqualTo(3);
        
        List<Transaction> allTransactions = transactionRepository.findAll();
        assertThat(allTransactions)
                .hasSize(3)
                .extracting(Transaction::getReference)
                .containsExactlyInAnyOrder("BULK-TX-001", "BULK-TX-002", "BULK-TX-003");
    }

    @Test
    @Transactional
    @DisplayName("Should filter transactions by status")
    void testFilterByStatus() {
        // Arrange
        createTestTransactions();

        // Act
        List<Transaction> pendingTransactions = transactionRepository.findByStatus(TransactionStatus.PENDING);
        List<Transaction> completedTransactions = transactionRepository.findByStatus(TransactionStatus.COMPLETED);

        // Assert
        assertThat(pendingTransactions).allMatch(t -> t.getStatus() == TransactionStatus.PENDING);
        assertThat(completedTransactions).allMatch(t -> t.getStatus() == TransactionStatus.COMPLETED);
    }

    @Test
    @Transactional
    @DisplayName("Should filter transactions by type")
    void testFilterByType() {
        // Arrange
        createTestTransactions();

        // Act
        List<Transaction> transferTransactions = transactionRepository.findByType(TransactionType.P2P_TRANSFER);

        // Assert
        assertThat(transferTransactions).allMatch(t -> t.getType() == TransactionType.P2P_TRANSFER);
    }

    @Test
    @Transactional
    @DisplayName("Should aggregate transaction amounts")
    void testAmountCalculations() {
        // Arrange
        BigDecimal amount1 = new BigDecimal("100.00");
        BigDecimal amount2 = new BigDecimal("250.50");
        BigDecimal amount3 = new BigDecimal("149.99");

        Transaction tx1 = createTransaction("AGG-TX-001", amount1);
        Transaction tx2 = createTransaction("AGG-TX-002", amount2);
        Transaction tx3 = createTransaction("AGG-TX-003", amount3);

        @SuppressWarnings({"null", "unused"})
        var saved = transactionRepository.saveAll(List.of(tx1, tx2, tx3));

        // Act
        List<Transaction> transactions = transactionRepository.findAll();
        BigDecimal totalAmount = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Assert
        BigDecimal expectedTotal = amount1.add(amount2).add(amount3);
        assertThat(totalAmount).isEqualByComparingTo(expectedTotal);
    }

    @Test
    @Transactional
    @DisplayName("Should maintain data isolation between test methods")
    void testDataIsolation_FirstCall() {
        // Act
        @SuppressWarnings({"null", "unused"})
        Transaction saved = transactionRepository.save(createTransaction("ISOLATION-TX-001", new BigDecimal("100.00")));

        // Assert
        assertThat(transactionRepository.count()).isEqualTo(1);
    }

    @Test
    @Transactional
    @DisplayName("Should have clean state in each test method")
    void testDataIsolation_SecondCall() {
        // Assert - Should be empty due to rollback of previous test
        assertThat(transactionRepository.count()).isEqualTo(0);
    }

    // ==================== Helper Methods ====================

    private void createTestTransactions() {
        LocalDateTime now = LocalDateTime.now();

        Transaction tx1 = Transaction.builder()
                .reference("TEST-TX-001")
                .senderWalletNumber("WALLET-001")
                .receiverWalletNumber("WALLET-002")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .status(TransactionStatus.PENDING)
                .type(TransactionType.P2P_TRANSFER)
                .channel(TransactionChannel.WEB)
                .createdAt(now)
                .build();

        Transaction tx2 = Transaction.builder()
                .reference("TEST-TX-002")
                .senderWalletNumber("WALLET-002")
                .receiverWalletNumber("WALLET-003")
                .amount(new BigDecimal("200.00"))
                .currency("USD")
                .status(TransactionStatus.COMPLETED)
                .type(TransactionType.P2P_TRANSFER)
                .channel(TransactionChannel.API)
                .createdAt(now.plusMinutes(5))
                .build();

        Transaction tx3 = Transaction.builder()
                .reference("TEST-TX-003")
                .senderWalletNumber("WALLET-003")
                .receiverWalletNumber("WALLET-001")
                .amount(new BigDecimal("150.00"))
                .currency("USD")
                .status(TransactionStatus.PENDING)
                .type(TransactionType.P2P_TRANSFER)
                .channel(TransactionChannel.MOBILE)
                .createdAt(now.plusMinutes(10))
                .build();

        @SuppressWarnings({"unused", "null"})
        var saved = transactionRepository.saveAll(List.of(tx1, tx2, tx3));
    }

    private Transaction createTransaction(String reference, BigDecimal amount) {
        return Transaction.builder()
                .reference(reference)
                .senderWalletNumber("WALLET-001")
                .receiverWalletNumber("WALLET-002")
                .amount(amount)
                .currency("USD")
                .status(TransactionStatus.PENDING)
                .type(TransactionType.P2P_TRANSFER)
                .channel(TransactionChannel.API)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
