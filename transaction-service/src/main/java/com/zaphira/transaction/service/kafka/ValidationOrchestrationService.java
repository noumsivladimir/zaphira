//package com.zaphira.transaction.service.kafka;
//
//import com.zaphira.transaction.kafka.event.TransactionValidationRequest;
//import com.zaphira.transaction.kafka.producer.ValidationRequestProducer;
//import com.zaphira.transaction.model.entities.Transaction;
//
//import com.zaphira.transaction.repository.kafka.ValidationRequestRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDateTime;
//import java.util.UUID;
//
///**
// * Service pour orchestrer la validation asynchrone via Kafka
// *
// * Responsabilités:
// * - Décider si une transaction nécessite validation asynchrone
// * - Construire et envoyer TransactionValidationRequest
// * - Gérer les tentatives et timeouts
// *
// * Point d'insertion: Appelé depuis TransactionServiceImpl après création
// * Garantie: Async publish, ne bloque pas la création de la transaction
// */
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class ValidationOrchestrationService {
//
//    private final ValidationRequestProducer validationRequestProducer;
//    private final ValidationRequestRepository validationRequestRepository;
//
//    /**
//     * Déclenche une validation asynchrone pour une transaction
//     *
//     * Appelé après création de la transaction si elle nécessite une autorisation
//     *
//     * @param transaction La transaction à valider
//     */
//    @Transactional
//    public void initiateAsyncValidation(Transaction transaction) {
//        try {
//            // Générer un correlationId unique
//            String correlationId = UUID.randomUUID().toString();
//
//            // Construire la requête de validation
//            TransactionValidationRequest request = TransactionValidationRequest.builder()
//                .correlationId(correlationId)
//                .transactionId(transaction.getId())
//                .reference(transaction.getReference())
//                .senderWalletNumber(transaction.getSenderWalletNumber())
//                .receiverWalletNumber(transaction.getReceiverWalletNumber())
//                .amount(transaction.getAmount())
//                .currency(transaction.getCurrency().name())
//                .feeAmount(transaction.getFeeAmount())
//                .type(transaction.getType().name())
//                .channel(transaction.getChannel().name())
//                .initialRiskScore(transaction.getRiskScore())
//                .initiatedAt(transaction.getInitiatedAt())
//                .deviceIdentifier(generateDeviceIdentifier())  // Anonymisé
//                .build();
//
//            // Enregistrer la requête dans la BD (pour idempotence)
//            com.zaphira.transaction.model.kafka.ValidationRequest dbRecord =
//                com.zaphira.transaction.model.kafka.ValidationRequest.builder()
//                    .correlationId(correlationId)
//                    .transactionId(transaction.getId())
//                    .requestedAt(LocalDateTime.now())
//                    .expiresAt(LocalDateTime.now().plusMinutes(5))  // Timeout 5 min
//                    .status("PENDING")
//                    .build();
//            validationRequestRepository.save(dbRecord);
//
//            // Publier sur Kafka (asynchrone)
//            validationRequestProducer.publishValidationRequest(request);
//
//            log.info(
//                "Initiated async validation for transaction: {}, correlationId: {}",
//                transaction.getId(),
//                correlationId
//            );
//
//        } catch (Exception e) {
//            log.error(
//                "Failed to initiate async validation for transaction: {}, error: {}",
//                transaction.getId(),
//                e.getMessage(),
//                e
//            );
//            // Ne pas bloquer la création si la validation échoue
//            // La transaction sera créée même sans validation asynchrone
//            // C'est un fallback pour la robustesse
//        }
//    }
//
//    /**
//     * Génère un device identifier anonymisé (no PII)
//     */
//    private String generateDeviceIdentifier() {
//        // Placeholder: en production, gérer via contexte request
//        return "anonymous";
//    }
//}
