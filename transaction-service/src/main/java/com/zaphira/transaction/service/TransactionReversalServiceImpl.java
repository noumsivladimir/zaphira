package com.zaphira.transaction.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TransactionReversalServiceImpl  {

//    private final TransactionRepository transactionRepository;
//    private final TransactionStatusHistoryRepository historyRepository;
//    private final TransactionMapper transactionMapper;
//    private final WalletService walletService;
//    private final TransactionEventPublisher eventPublisher;
//
//    @Override
//    public TransactionDTO reverseTransaction(String reference,
//                                             String reason,
//                                             String initiatedBy) {
//
//        Transaction original = transactionRepository.findByReferenceWithLock(reference)
//                .orElseThrow(() -> new TransactionExceptions.TransactionNotFoundException(reference));
//
//        validateReversal(original);
//
//        Transaction reversal = createReversalTransaction(original, reason, initiatedBy);
//
//        executeReversalWalletOperations(original, reversal);
//
//        markOriginalAsReversed(original, reversal, initiatedBy, reason);
//
//        transactionRepository.save(reversal);
//        transactionRepository.save(original);
//
//        eventPublisher.publishTransactionReversed(reversal);
//
//        log.info("TX {} reversed by {}", reference, initiatedBy);
//
//        return transactionMapper.toDTO(reversal);
//    }
//
//    /* =========================================================
//       INTERNALS
//       ========================================================= */
//
//    private void validateReversal(Transaction tx) {
//
//        if (tx.getStatus() != TransactionStatus.COMPLETED) {
//            throw new TransactionExceptions.InvalidTransactionStatusException(
//                    "Only COMPLETED transactions can be reversed");
//        }
//
//        if (Boolean.TRUE.equals(tx.getIsReversed())) {
//            throw new TransactionExceptions.BusinessException(
//                    "Transaction already reversed");
//        }
//    }
//
//    private Transaction createReversalTransaction(Transaction original,
//                                                  String reason,
//                                                  String initiatedBy) {
//
//        return Transaction.builder()
//                .reference(generateReversalReference(original.getReference()))
//                .type(TransactionType.REVERSAL)
//                .category(original.getCategory())
//                .amount(original.getAmount())
//                .feeAmount(BigDecimal.ZERO)
//                .currency(original.getCurrency())
//                .senderWallet(original.getReceiverWallet())
//                .receiverWallet(original.getSenderWallet())
//                .senderWalletNumber(original.getReceiverWalletNumber())
//                .receiverWalletNumber(original.getSenderWalletNumber())
//                .relatedTransactionId(original.getId())
//                .description("REVERSAL - " + reason)
//                .status(TransactionStatus.PROCESSING)
//                .initiatedBy(initiatedBy)
//                .build();
//    }
//
//    private void executeReversalWalletOperations(Transaction original,
//                                                 Transaction reversal) {
//
//        BalanceOperationRequest request = BalanceOperationRequest.builder()
//                .amount(reversal.getAmount())
//                .transactionReference(reversal.getReference())
//                .description(reversal.getDescription())
//                .build();
//
//        switch (original.getType()) {
//
//            case TRANSFER -> {
//                walletService.debitWallet(
//                        original.getReceiverWalletNumber(), request);
//                walletService.creditWallet(
//                        original.getSenderWalletNumber(), request);
//            }
//
//            case DEBIT, WITHDRAWAL -> {
//                walletService.creditWallet(
//                        original.getSenderWalletNumber(), request);
//            }
//
//            case CREDIT, DEPOSIT -> {
//                walletService.debitWallet(
//                        original.getReceiverWalletNumber(), request);
//            }
//
//            default -> throw new TransactionExceptions.BusinessException(
//                    "Reversal not supported for type " + original.getType());
//        }
//
//        reversal.setStatus(TransactionStatus.COMPLETED);
//    }
//
//    private void markOriginalAsReversed(Transaction original,
//                                        Transaction reversal,
//                                        String initiatedBy,
//                                        String reason) {
//
//        original.setIsReversed(true);
//        original.setReversedAt(java.time.LocalDateTime.now());
//        original.setRelatedTransactionId(reversal.getId());
//
//        historyRepository.save(
//                com.zaphira.transaction.model.entities.TransactionStatusHistory.builder()
//                        .transaction(original)
//                        .status(TransactionStatus.REVERSED)
//                        .previousStatus(original.getStatus())
//                        .changedBy(initiatedBy)
//                        .reason(reason)
//                        .changedAt(java.time.LocalDateTime.now())
//                        .build()
//        );
//
//        original.setStatus(TransactionStatus.REVERSED);
//    }
//
//    private String generateReversalReference(String originalRef) {
//        return originalRef + "-REV";
//    }
}
