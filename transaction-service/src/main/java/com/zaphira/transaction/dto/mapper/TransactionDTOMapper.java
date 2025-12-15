package com.zaphira.transaction.dto.mapper;

import com.zaphira.transaction.dto.AuthorizationInfoResponse;
import com.zaphira.transaction.dto.TransactionDTO;
import com.zaphira.transaction.dto.TransactionEventDTO;
import com.zaphira.transaction.dto.TransactionResponseDTO;
import com.zaphira.transaction.dto.TransactionStateHistoryDTO;
import com.zaphira.transaction.dto.WalletSummaryDTO;
import com.zaphira.transaction.model.Transaction;
import com.zaphira.transaction.model.TransactionStateHistory;
import com.zaphira.transaction.model.enums.TransactionStatus;
import com.zaphira.common.model.entities.Wallet;
import org.springframework.stereotype.Component;

/**
 * Mapper pour convertir les entités Transaction en DTO
 * Gère le mapping des relations lazy et exclut les propriétés Hibernate
 */
@Component
public class TransactionDTOMapper {

    /**
     * Convertit une entité Transaction en TransactionDTO
     * @param transaction l'entité à mapper
     * @return le DTO correspondant
     */
    public TransactionDTO toDTO(Transaction transaction) {
        if (transaction == null) {
            return null;
        }

        return TransactionDTO.builder()
                .id(transaction.getId())
                .reference(transaction.getReference())
                .senderWalletNumber(transaction.getSenderWalletNumber())
                .receiverWalletNumber(transaction.getReceiverWalletNumber())
                .senderWallet(mapWalletToSummary(transaction.getSenderWallet()))
                .receiverWallet(mapWalletToSummary(transaction.getReceiverWallet()))
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .feeAmount(transaction.getFeeAmount())
                .feeCurrency(transaction.getFeeCurrency())
                .feeType(transaction.getFeeType())
                .type(transaction.getType())
                .status(transaction.getStatus())
                .channel(transaction.getChannel())
                .description(transaction.getDescription())
                .authorizationMethod(transaction.getAuthorizationMethod())
                .authorizationRequired(transaction.getAuthorizationRequired())
                .retryCount(transaction.getRetryCount())
                .maxRetry(transaction.getMaxRetry())
                .scheduled(transaction.getScheduled())
                .scheduledFor(transaction.getScheduledFor())
                .metadata(transaction.getMetadata())
                .initiatedBy(transaction.getInitiatedBy())
                .lastUpdatedBy(transaction.getLastUpdatedBy())
                .riskScore(transaction.getRiskScore())
                .complianceStatus(transaction.getComplianceStatus())
                .initiatedAt(transaction.getInitiatedAt())
                .pendingAt(transaction.getPendingAt())
                .authorizedAt(transaction.getAuthorizedAt())
                .processingAt(transaction.getProcessingAt())
                .completedAt(transaction.getCompletedAt())
                .failedAt(transaction.getFailedAt())
                .cancelledAt(transaction.getCancelledAt())
                .reversedAt(transaction.getReversedAt())
                .refundedAt(transaction.getRefundedAt())
                .expiredAt(transaction.getExpiredAt())
                .onHoldAt(transaction.getOnHoldAt())
                .underReviewAt(transaction.getUnderReviewAt())
                .build();
    }

    /**
     * Convertit une entité Transaction en TransactionResponseDTO
     * @param transaction l'entité à mapper
     * @return le DTO de réponse correspondant
     */
    public TransactionResponseDTO toResponseDTO(Transaction transaction) {
        if (transaction == null) {
            return null;
        }

        return TransactionResponseDTO.builder()
                .id(transaction.getId())
                .reference(transaction.getReference())
                .senderWalletNumber(transaction.getSenderWalletNumber())
                .receiverWalletNumber(transaction.getReceiverWalletNumber())
                .senderWallet(mapWalletToSummary(transaction.getSenderWallet()))
                .receiverWallet(mapWalletToSummary(transaction.getReceiverWallet()))
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .feeAmount(transaction.getFeeAmount())
                .feeCurrency(transaction.getFeeCurrency())
                .feeType(transaction.getFeeType())
                .type(transaction.getType())
                .status(transaction.getStatus())
                .channel(transaction.getChannel())
                .description(transaction.getDescription())
                .authorizationMethod(transaction.getAuthorizationMethod())
                .authorizationRequired(transaction.getAuthorizationRequired())
                .retryCount(transaction.getRetryCount())
                .maxRetry(transaction.getMaxRetry())
                .scheduled(transaction.getScheduled())
                .scheduledFor(transaction.getScheduledFor())
                .metadata(transaction.getMetadata())
                .initiatedBy(transaction.getInitiatedBy())
                .lastUpdatedBy(transaction.getLastUpdatedBy())
                .riskScore(transaction.getRiskScore())
                .complianceStatus(transaction.getComplianceStatus())
                .initiatedAt(transaction.getInitiatedAt())
                .pendingAt(transaction.getPendingAt())
                .authorizedAt(transaction.getAuthorizedAt())
                .processingAt(transaction.getProcessingAt())
                .completedAt(transaction.getCompletedAt())
                .failedAt(transaction.getFailedAt())
                .cancelledAt(transaction.getCancelledAt())
                .reversedAt(transaction.getReversedAt())
                .refundedAt(transaction.getRefundedAt())
                .expiredAt(transaction.getExpiredAt())
                .onHoldAt(transaction.getOnHoldAt())
                .underReviewAt(transaction.getUnderReviewAt())
                .build();
    }

    /**
     * Convertit une entité TransactionStateHistory en TransactionStateHistoryDTO
     * @param history l'entité à mapper
     * @return le DTO correspondant
     */
    public TransactionStateHistoryDTO toDTO(TransactionStateHistory history) {
        if (history == null) {
            return null;
        }

        return TransactionStateHistoryDTO.builder()
                .id(history.getId())
                .transactionId(history.getTransaction() != null ? history.getTransaction().getId() : null)
                .status(history.getStatus())
                .changedBy(history.getChangedBy())
                .reason(history.getReason())
                .changedAt(history.getChangedAt())
                .build();
    }

    /**
     * Convertit une entité Transaction en AuthorizationInfoResponse
     * @param transaction l'entité à mapper
     * @return le DTO correspondant
     */
    public AuthorizationInfoResponse toAuthorizationInfoResponse(Transaction transaction) {
        if (transaction == null) {
            return null;
        }

        return AuthorizationInfoResponse.builder()
                .transactionId(transaction.getId())
                .status(transaction.getStatus())
                .authorizationMethod(transaction.getAuthorizationMethod())
                .authorizationRequired(transaction.getAuthorizationRequired())
                .pendingAt(transaction.getPendingAt())
                .authorizedAt(transaction.getAuthorizedAt())
                .build();
    }

    /**
     * Convertit une entité Transaction en TransactionEventDTO pour Kafka
     * @param transaction l'entité à mapper
     * @param eventType le type d'événement (CREATED, STATUS_CHANGED, AUTHORIZED, FAILED)
     * @param previousStatus le statut précédent (nullable)
     * @return le DTO d'événement correspondant
     */
    public TransactionEventDTO toEventDTO(Transaction transaction, String eventType, TransactionStatus previousStatus) {
        if (transaction == null) {
            return null;
        }

        return TransactionEventDTO.builder()
                .transactionId(transaction.getId())
                .reference(transaction.getReference())
                .senderWalletNumber(transaction.getSenderWalletNumber())
                .receiverWalletNumber(transaction.getReceiverWalletNumber())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .type(transaction.getType())
                .status(transaction.getStatus())
                .previousStatus(previousStatus)
                .channel(transaction.getChannel() != null ? transaction.getChannel().toString() : null)
                .timestamp(transaction.getLastUpdatedAt() != null ? transaction.getLastUpdatedAt() : transaction.getInitiatedAt())
                .eventType(eventType)
                .metadata(transaction.getMetadata())
                .initiatedBy(transaction.getInitiatedBy())
                .changedBy(transaction.getLastUpdatedBy())
                .feeAmount(transaction.getFeeAmount())
                .feeCurrency(transaction.getFeeCurrency())
                .riskScore(transaction.getRiskScore())
                .build();
    }

    /**
     * Convertit une entité Transaction en TransactionEventDTO pour Kafka (sans statut précédent)
     * @param transaction l'entité à mapper
     * @param eventType le type d'événement
     * @return le DTO d'événement correspondant
     */
    public TransactionEventDTO toEventDTO(Transaction transaction, String eventType) {
        return toEventDTO(transaction, eventType, null);
    }

    /**
     * Mappe une entité Wallet vers un WalletSummaryDTO
     * Exclut toutes les relations lazy
     * @param wallet l'entité à mapper
     * @return le DTO résumé
     */
    private WalletSummaryDTO mapWalletToSummary(Wallet wallet) {
        if (wallet == null) {
            return null;
        }

        return WalletSummaryDTO.builder()
                .walletNumber(wallet.getWalletNumber())
                .balance(wallet.getBalance())
                .currency(wallet.getCurrency())
                .build();
    }
}

