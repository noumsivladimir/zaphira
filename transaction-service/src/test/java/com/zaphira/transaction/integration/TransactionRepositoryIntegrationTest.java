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
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test for TransactionRepository using SQLite in-memory database.
 * 
 * This test verifies basic CRUD operations and JPA queries.
 * Uses @DataJpaTest for focused testing on JPA layer.
 * 
 * Profile: "test" activates SQLite in-memory database via application-test.yml
 */
@DataJpaTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Transaction Repository - SQLite Integration Tests")
class TransactionRepositoryIntegrationTest {

    @Autowired
    private TransactionRepository transactionRepository;

    private Transaction testTransaction;

    @BeforeEach
    void setUp() {
        // Create a test transaction
        testTransaction = Transaction.builder()
                .reference("TXN-" + System.currentTimeMillis())
                .senderWalletNumber("WALLET-001")
                .receiverWalletNumber("WALLET-002")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .status(TransactionStatus.PENDING)
                .type(TransactionType.P2P_TRANSFER)
                .channel(TransactionChannel.MOBILE)
                .description("Test transaction")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should save and retrieve transaction by ID")
    void testSaveAndFindById() {
        // Arrange & Act
        @SuppressWarnings("null")
        Transaction savedTransaction = transactionRepository.save(testTransaction);

        // Assert
        assertThat(savedTransaction.getId()).isNotNull();
        
        @SuppressWarnings("null")
        Optional<Transaction> retrievedTransaction = transactionRepository.findById(savedTransaction.getId());
        
        assertThat(retrievedTransaction).isPresent();
        assertThat(retrievedTransaction.get().getReference()).isEqualTo(testTransaction.getReference());
        assertThat(retrievedTransaction.get().getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("Should find transactions by sender wallet number")
    void testFindBySenderWalletNumber() {
        // Arrange
        @SuppressWarnings("null")
        Transaction tx1 = transactionRepository.save(testTransaction);
        
        Transaction secondTransaction = Transaction.builder()
                .reference("TXN-2-" + System.currentTimeMillis())
                .senderWalletNumber("WALLET-001")
                .receiverWalletNumber("WALLET-003")
                .amount(new BigDecimal("50.00"))
                .currency("USD")
                .status(TransactionStatus.COMPLETED)
                .type(TransactionType.P2P_TRANSFER)
                .channel(TransactionChannel.WEB)
                .description("Second test transaction")
                .createdAt(LocalDateTime.now())
                .build();
        
        transactionRepository.save(secondTransaction);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        var transactionsPage = transactionRepository.findBySenderWalletNumber("WALLET-001", pageable);
        List<Transaction> transactions = transactionsPage.getContent();

        // Assert
        assertThat(transactions).hasSize(2);
        assertThat(transactions).allMatch(t -> t.getSenderWalletNumber().equals("WALLET-001"));
    }

    @Test
    @DisplayName("Should update transaction status")
    void testUpdateTransactionStatus() {
        // Arrange
        @SuppressWarnings("null")
        Transaction savedTransaction = transactionRepository.save(testTransaction);
        @SuppressWarnings("null")
        Long transactionId = savedTransaction.getId();

        // Act
        @SuppressWarnings("null")
        Optional<Transaction> retrievedTransaction = transactionRepository.findById(transactionId);
        assertThat(retrievedTransaction).isPresent();
        
        Transaction transaction = retrievedTransaction.get();
        transaction.setStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(transaction);

        // Assert
        @SuppressWarnings("null")
        Optional<Transaction> updatedTransaction = transactionRepository.findById(transactionId);
        assertThat(updatedTransaction)
                .isPresent()
                .get()
                .satisfies(t -> assertThat(t.getStatus()).isEqualTo(TransactionStatus.COMPLETED));
    }

    @Test
    @DisplayName("Should delete transaction by ID")
    void testDeleteTransaction() {
        @SuppressWarnings("null")
        Transaction savedTransaction = transactionRepository.save(testTransaction);
        @SuppressWarnings("null")
        Long transactionId = savedTransaction.getId();

        // Act
        transactionRepository.deleteById(transactionId);

        // Assert
        @SuppressWarnings("null")
        Optional<Transaction> deletedTransaction = transactionRepository.findById(transactionId);
        assertThat(deletedTransaction).isEmpty();
    }

    @Test
    @DisplayName("Should find transactions by date range")
    void testFindByDateRange() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minusHours(1);
        LocalDateTime oneHourLater = now.plusHours(1);

        Transaction tx1 = Transaction.builder()
                .reference("TXN-RANGE-1")
                .senderWalletNumber("WALLET-001")
                .receiverWalletNumber("WALLET-002")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .status(TransactionStatus.PENDING)
                .type(TransactionType.P2P_TRANSFER)
                .channel(TransactionChannel.API)
                .createdAt(now)
                .build();

        Transaction tx2 = Transaction.builder()
                .reference("TXN-RANGE-2")
                .senderWalletNumber("WALLET-003")
                .receiverWalletNumber("WALLET-004")
                .amount(new BigDecimal("200.00"))
                .currency("USD")
                .status(TransactionStatus.COMPLETED)
                .type(TransactionType.P2P_TRANSFER)
                .channel(TransactionChannel.API)
                .createdAt(oneHourLater.plusHours(2))
                .build();

        transactionRepository.save(tx1);
        transactionRepository.save(tx2);

        // Act
        List<Transaction> transactionsInRange = transactionRepository.findByCreatedAtBetween(oneHourAgo, oneHourLater);

        // Assert
        assertThat(transactionsInRange).hasSize(1);
        assertThat(transactionsInRange.get(0).getReference()).isEqualTo("TXN-RANGE-1");
    }

    @Test
    @DisplayName("Should enforce unique constraint on transaction reference")
    void testUniqueReferenceConstraint() {
        // Arrange
        String uniqueReference = "UNIQUE-TXN-" + System.currentTimeMillis();
        
        Transaction tx1 = Transaction.builder()
                .reference(uniqueReference)
                .senderWalletNumber("WALLET-001")
                .receiverWalletNumber("WALLET-002")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .status(TransactionStatus.PENDING)
                .type(TransactionType.P2P_TRANSFER)
                .channel(TransactionChannel.API)
                .createdAt(LocalDateTime.now())
                .build();

        @SuppressWarnings("null")
        Transaction savedTx1 = transactionRepository.save(tx1);

        // Act & Assert
        Transaction tx2 = Transaction.builder()
                .reference(uniqueReference)
                .senderWalletNumber("WALLET-003")
                .receiverWalletNumber("WALLET-004")
                .amount(new BigDecimal("200.00"))
                .currency("USD")
                .status(TransactionStatus.PENDING)
                .type(TransactionType.P2P_TRANSFER)
                .channel(TransactionChannel.API)
                .createdAt(LocalDateTime.now())
                .build();

        assertThatThrownBy(() -> {
            @SuppressWarnings("null")
            Transaction saved = transactionRepository.save(tx2);
        }).isNotNull();
    }

    @Test
    @DisplayName("Should count transactions")
    void testCountTransactions() {
        @SuppressWarnings("null")
        Transaction tx1 = transactionRepository.save(testTransaction);
        @SuppressWarnings("null")
        Transaction tx2 = transactionRepository.save(testTransaction);
        @SuppressWarnings("null")
        Transaction tx3 = transactionRepository.save(testTransaction.toBuilder()
                .reference("TXN-2-" + System.currentTimeMillis())
                .build());

        // Act
        long count = transactionRepository.count();

        // Assert
        assertThat(count).isGreaterThanOrEqualTo(2);
    }
}
