package com.zaphira.transaction.service;

import com.zaphira.transaction.config.FeeProperties;
import com.zaphira.transaction.config.LimitProperties;
import com.zaphira.transaction.dto.AuthorizationValidationRequest;
import com.zaphira.transaction.dto.TransactionRequest;
import com.zaphira.transaction.dto.UpdateStatusRequest;
import com.zaphira.transaction.integration.wallet.WalletClient;
import com.zaphira.transaction.model.Transaction;
import com.zaphira.transaction.model.TransactionStateHistory;
import com.zaphira.transaction.model.enums.AuthorizationMethod;
import com.zaphira.transaction.model.enums.TransactionChannel;
import com.zaphira.transaction.model.enums.TransactionStatus;
import com.zaphira.transaction.model.enums.TransactionType;
import com.zaphira.transaction.repository.TransactionRepository;
import com.zaphira.transaction.repository.TransactionStateHistoryRepository;
import com.zaphira.transaction.service.authorization.TransactionAuthorizationService;
import com.zaphira.transaction.service.compliance.ComplianceService;
import com.zaphira.transaction.service.fee.FeeCalculationResult;
import com.zaphira.transaction.service.fee.FeeService;
import com.zaphira.transaction.service.limit.LimitEvaluationResult;
import com.zaphira.transaction.service.limit.TransactionLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionStateHistoryRepository stateHistoryRepository;

    @Mock
    private TransactionValidationService validationService;

    @Mock
    private WalletClient walletClient;

    @Mock
    private TransactionLimitService limitService;

    @Mock
    private TransactionAuthorizationService authorizationService;

    private TransactionService transactionService;
    private LimitProperties limitProperties;
    private FeeProperties feeProperties;

    @Mock
    private FeeService feeService;

    @Mock
    private ComplianceService complianceService;

    @BeforeEach
    void setup() {
        limitProperties = new LimitProperties();
        feeProperties = new FeeProperties();
        transactionService = new TransactionService(
                transactionRepository,
                stateHistoryRepository,
                validationService,
                limitService,
                feeService,
                authorizationService,
                complianceService,
                limitProperties,
                feeProperties,
                walletClient
        );
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(stateHistoryRepository.save(any(TransactionStateHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createTransaction_shouldValidateAndProcess() {
        TransactionRequest request = new TransactionRequest();
        request.setSenderWalletNumber("W1");
        request.setReceiverWalletNumber("W2");
        request.setAmount(BigDecimal.valueOf(100));
        request.setCurrency("XOF");
        request.setType(TransactionType.P2P_TRANSFER);
        request.setChannel(TransactionChannel.MOBILE);
        request.setDescription("Test");
        request.setProcessInstantly(true);
        request.setRequestedBy("tester");

        when(limitService.evaluateAuthorizationNeed(request))
                .thenReturn(LimitEvaluationResult.builder()
                        .authorizationRequired(false)
                        .reason("below threshold")
                        .build());
        when(feeService.calculateFee(request))
                .thenReturn(FeeCalculationResult.builder()
                        .feeAmount(BigDecimal.valueOf(1))
                        .feeCurrency("XOF")
                        .feeType("TEST")
                        .build());

        Transaction saved = transactionService.createTransaction(request);

        assertThat(saved.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(saved.getFeeAmount()).isEqualTo(BigDecimal.valueOf(1));
        verify(validationService).validateInitiation(request);
        verify(walletClient).executeTransfer(any());
        verify(stateHistoryRepository, atLeastOnce()).save(any(TransactionStateHistory.class));
    }

    @Test
    void createTransaction_whenAuthorizationRequired_shouldStayPending() {
        TransactionRequest request = new TransactionRequest();
        request.setSenderWalletNumber("W1");
        request.setReceiverWalletNumber("W2");
        request.setAmount(BigDecimal.valueOf(5000));
        request.setCurrency("XOF");
        request.setType(TransactionType.P2P_TRANSFER);
        request.setChannel(TransactionChannel.MOBILE);
        request.setDescription("High amount");
        request.setProcessInstantly(true);
        request.setRequestedBy("tester");

        when(limitService.evaluateAuthorizationNeed(request))
                .thenReturn(LimitEvaluationResult.builder()
                        .authorizationRequired(true)
                        .method(AuthorizationMethod.OTP)
                        .reason("above threshold")
                        .build());
        when(feeService.calculateFee(request))
                .thenReturn(FeeCalculationResult.builder()
                        .feeAmount(BigDecimal.ONE)
                        .feeCurrency("XOF")
                        .feeType("TEST")
                        .build());

        Transaction saved = transactionService.createTransaction(request);

        assertThat(saved.getStatus()).isEqualTo(TransactionStatus.PENDING);
        verify(authorizationService).createAuthorization(saved, AuthorizationMethod.OTP, "tester");
        verify(walletClient, never()).executeTransfer(any());
    }

    @Test
    void authorizeTransaction_shouldProcessAfterApproval() {
        Transaction tx = Transaction.builder()
                .id(2L)
                .authorizationRequired(true)
                .authorizationMethod(AuthorizationMethod.OTP)
                .senderWalletNumber("W1")
                .receiverWalletNumber("W2")
                .amount(BigDecimal.valueOf(100))
                .currency("XOF")
                .status(TransactionStatus.PENDING)
                .build();
        when(transactionRepository.findById(2L)).thenReturn(Optional.of(tx));

        AuthorizationValidationRequest request = new AuthorizationValidationRequest();
        request.setMethod(AuthorizationMethod.OTP);
        request.setCode("123456");
        request.setAuthorizedBy("approver");

        Transaction result = transactionService.authorizeTransaction(2L, request);

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        verify(authorizationService).approveAuthorization(2L, AuthorizationMethod.OTP, "123456", "approver");
        verify(walletClient).executeTransfer(any());
    }

    @Test
    void getTransaction_whenNotFound_shouldThrow() {
        when(transactionRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class, () -> transactionService.getTransaction(1L));
    }

    @Test
    void updateTransactionStatus_shouldChangeState() {
        Transaction tx = Transaction.builder()
                .id(1L)
                .status(TransactionStatus.INITIATED)
                .build();
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(tx));

        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus(TransactionStatus.PENDING);
        request.setChangedBy("system");
        request.setReason("manual update");

        Transaction updated = transactionService.updateTransactionStatus(1L, request);
        assertThat(updated.getStatus()).isEqualTo(TransactionStatus.PENDING);
        verify(stateHistoryRepository, atLeastOnce()).save(any(TransactionStateHistory.class));
    }

    @Test
    void cancelTransaction_shouldFailWhenCompleted() {
        Transaction tx = Transaction.builder()
                .id(1L)
                .status(TransactionStatus.COMPLETED)
                .build();
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(tx));

        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus(TransactionStatus.CANCELLED);
        request.setChangedBy("user");

        assertThrows(IllegalStateException.class, () -> transactionService.cancelTransaction(1L, request));
    }
}


