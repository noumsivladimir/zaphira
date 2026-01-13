//package com.zaphira.transaction.service.kafka;
//
//import com.zaphira.transaction.kafka.event.TransactionValidationResult;
//import com.zaphira.transaction.model.entities.Transaction;
//import com.zaphira.transaction.model.enums.ComplianceStatus;
//import com.zaphira.transaction.model.enums.TransactionStatus;
//import com.zaphira.transaction.repository.TransactionRepository;
//import com.zaphira.transaction.repository.kafka.ValidationRequestRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDateTime;
//
///**
// * Service coordinateur pour traiter les résultats de validation asynchrones
// *
// * Responsabilités:
// * - Valider l'idempotence via correlationId
// * - Mapper ValidationResult → Transaction state
// * - Gérer les transitions d'état valides
// * - Mettre à jour les timestamps et audit
// * - Protéger contre les conditions de concurrence
// *
// * Garantie: Idempotent (même message réceptionné multiple fois = effet identique)
// */
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class ValidationCoordinatorService {
//
//    private final TransactionRepository transactionRepository;
//    private final ValidationRequestRepository validationRequestRepository;
//
//    /**
//     * Traite un résultat de validation et met à jour l'état de la transaction
//     *
//     * Flux:
//     * 1. Vérifier idempotence (correlationId)
//     * 2. Récupérer et valider la transaction
//     * 3. Vérifier l'état actuel (éviter transitions invalides)
//     * 4. Mapper le résultat vers un statut Transaction
//     * 5. Mettre à jour la DB en transaction
//     *
//     * @param result Le résultat de validation reçu de Kafka
//     * @throws IllegalArgumentException si erreur métier
//     */
//    @Transactional
//    public void processValidationResult(TransactionValidationResult result) {
//
//        // 1. Vérifier idempotence
//        boolean alreadyProcessed = validationRequestRepository.existsByCorrelationId(result.getCorrelationId());
//        if (alreadyProcessed) {
//            log.info(
//                "Validation result already processed for correlationId: {}, transaction: {}. Skipping.",
//                result.getCorrelationId(),
//                result.getTransactionId()
//            );
//            return;
//        }
//
//        // 2. Récupérer la transaction
//        Transaction transaction = transactionRepository
//            .findById(result.getTransactionId())
//            .orElseThrow(() -> new IllegalArgumentException(
//                "Transaction not found: " + result.getTransactionId()
//            ));
//
//        // 3. Vérifier l'état actuel (doit être PENDING pour une validation)
//        TransactionStatus currentStatus = transaction.getStatus();
//        if (!isValidPreviousStatus(currentStatus)) {
//            throw new IllegalArgumentException(
//                String.format(
//                    "Invalid transaction state for validation update. " +
//                    "Current status: %s, Transaction: %s. Expected: PENDING",
//                    currentStatus, result.getTransactionId()
//                )
//            );
//        }
//
//        // 4. Mapper le résultat de validation au statut Transaction
//        TransactionStatus newStatus = mapValidationStatusToTransactionStatus(result.getValidationStatus());
//        ComplianceStatus newComplianceStatus = mapComplianceStatus(result.getComplianceStatus());
//
//        // 5. Mettre à jour la transaction
//        transaction.setStatus(newStatus);
//        transaction.setComplianceStatus(newComplianceStatus);
//        transaction.setRiskScore(result.getRiskScoreFinal() != null ? result.getRiskScoreFinal() : transaction.getRiskScore());
//        transaction.setLastUpdatedBy("VALIDATION_SERVICE:" + result.getValidatorId());
//        transaction.setLastUpdatedAt(LocalDateTime.now());
//
//        // Mettre à jour les timestamps selon le statut
//        updateTimestampsBasedOnStatus(transaction, newStatus);
//
//        // Persister la transaction
//
//
//        // Enregistrer la demande comme traitée (pour idempotence)
//        validationRequestRepository.markAsProcessed(result.getCorrelationId());
//
//        log.info(
//            "Successfully updated transaction: {}, newStatus: {}, " +
//            "complianceStatus: {}, correlationId: {}, validatorId: {}",
//            result.getTransactionId(),
//            newStatus,
//            newComplianceStatus,
//            result.getCorrelationId(),
//            result.getValidatorId()
//        );
//    }
//
//    /**
//     * Vérifie si l'état actuel permet une mise à jour de validation
//     * Une transaction peut être mise à jour si elle est en attente d'autorisation ou de vérification
//     */
//    private boolean isValidPreviousStatus(TransactionStatus status) {
//        return status == TransactionStatus.PENDING ||
//               status == TransactionStatus.PROCESSING ;
////               status == TransactionStatus.UNDER_REVIEW;
//    }
//
//    /**
//     * Mappe le statut de validation Kafka vers un statut Transaction
//     */
//    private TransactionStatus mapValidationStatusToTransactionStatus(
//        TransactionValidationResult.ValidationStatus validationStatus
//    ) {
//        return switch (validationStatus) {
//            case APPROVED -> TransactionStatus.AUTHORIZED;
//            case REJECTED -> TransactionStatus.FAILED;
//            case EXPIRED -> TransactionStatus.EXPIRED;
//        };
//    }
//
//    /**
//     * Mappe le statut de conformité Kafka vers un statut Transaction
//     */
//    private ComplianceStatus mapComplianceStatus(
//        TransactionValidationResult.ComplianceStatus complianceStatus
//    ) {
//        if (complianceStatus == null) {
//            return ComplianceStatus.CLEAR;
//        }
//
//        return switch (complianceStatus) {
//            case CLEAR -> ComplianceStatus.CLEAR;
//            case FLAGGED -> ComplianceStatus.UNDER_REVIEW;  // Map FLAGGED to UNDER_REVIEW (monitoring)
//            case BLOCKED -> ComplianceStatus.BLOCKED;
//        };
//    }
//
//    /**
//     * Met à jour les timestamps pertinents selon le nouveau statut
//     */
//    private void updateTimestampsBasedOnStatus(Transaction transaction, TransactionStatus newStatus) {
//        LocalDateTime now = LocalDateTime.now();
//
//        switch (newStatus) {
//            case AUTHORIZED:
//                transaction.setAuthorizedAt(now);
//                break;
//            case FAILED:
//                transaction.setFailedAt(now);
//                break;
//            case EXPIRED:
//                transaction.setExpiredAt(now);
//                break;
////            case UNDER_REVIEW:
////                transaction.setUnderReviewAt(now);
////                break;
//            default:
//                // Autres statuts ne changent pas les timestamps
//                break;
//        }
//    }
//}
