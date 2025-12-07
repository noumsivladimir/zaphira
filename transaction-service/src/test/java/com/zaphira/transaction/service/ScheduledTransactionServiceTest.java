package com.zaphira.transaction.service;

import com.zaphira.transaction.dto.ScheduledTransactionRequest;
import com.zaphira.transaction.dto.ScheduledTransactionResponse;
import com.zaphira.transaction.dto.TransactionRequest;
import com.zaphira.transaction.model.ScheduledTransaction;
import com.zaphira.transaction.model.Transaction;
import com.zaphira.transaction.model.enums.ScheduledTransactionStatus;
import com.zaphira.transaction.model.enums.TransactionChannel;
import com.zaphira.transaction.model.enums.TransactionType;
import com.zaphira.transaction.repository.ScheduledTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduledTransactionServiceTest {

    @Mock
    private ScheduledTransactionRepository repository;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private ScheduledTransactionService scheduledTransactionService;

    @BeforeEach
    void setup() {
        when(repository.save(any(ScheduledTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void create_shouldPersistScheduledTransaction() {
        ScheduledTransactionRequest request = new ScheduledTransactionRequest();
        request.setSenderWalletNumber("W1");
        request.setReceiverWalletNumber("W2");
        request.setAmount(BigDecimal.valueOf(100));
        request.setCurrency("XOF");
        request.setType(TransactionType.P2P_TRANSFER);
        request.setChannel(TransactionChannel.MOBILE);
        request.setDescription("scheduled");
        request.setRequestedBy("tester");
        request.setScheduledFor(LocalDateTime.now().plusMinutes(10));

        ScheduledTransactionResponse response = scheduledTransactionService.create(request);

        assertThat(response.getStatus()).isEqualTo(ScheduledTransactionStatus.PENDING);
        verify(repository).save(any(ScheduledTransaction.class));
    }

    @Test
    void executeScheduledTransaction_shouldCreateTransaction() {
        ScheduledTransaction scheduled = ScheduledTransaction.builder()
                .id(1L)
                .senderWalletNumber("W1")
                .receiverWalletNumber("W2")
                .amount(BigDecimal.valueOf(50))
                .currency("XOF")
                .type(TransactionType.P2P_TRANSFER)
                .channel(TransactionChannel.MOBILE)
                .requestedBy("tester")
                .scheduledFor(LocalDateTime.now().minusMinutes(1))
                .status(ScheduledTransactionStatus.PENDING)
                .build();

        Transaction tx = Transaction.builder()
                .id(99L)
                .build();
        when(transactionService.createTransaction(any(TransactionRequest.class))).thenReturn(tx);

        scheduledTransactionService.executeScheduledTransaction(scheduled);

        assertThat(scheduled.getStatus()).isEqualTo(ScheduledTransactionStatus.COMPLETED);
        assertThat(scheduled.getExecutedTransactionId()).isEqualTo(99L);
        verify(repository, atLeastOnce()).save(scheduled);
    }

    @Test
    void processDueSchedules_shouldCallExecuteOnDueOnes() {
        ScheduledTransaction s1 = ScheduledTransaction.builder()
                .id(1L)
                .scheduledFor(LocalDateTime.now().minusMinutes(1))
                .status(ScheduledTransactionStatus.PENDING)
                .build();
        when(repository.findDue(any(LocalDateTime.class))).thenReturn(List.of(s1));

        scheduledTransactionService.processDueSchedules();

        verify(repository, atLeastOnce()).save(any(ScheduledTransaction.class));
    }
}


