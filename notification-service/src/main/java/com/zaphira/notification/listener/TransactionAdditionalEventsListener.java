package com.zaphira.notification.listener;

import com.zaphira.transaction.kafka.event.TransactionRefundedEvent;
import com.zaphira.transaction.kafka.event.TransactionReversedEvent;
import com.zaphira.transaction.kafka.event.TransactionValidationResult;
import com.zaphira.notification.service.EmailService;
//import com.zaphira.notification.service.TelegramService;
import com.zaphira.notification.service.UserServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
@Slf4j
public class TransactionAdditionalEventsListener {

    private final EmailService emailService;
    //private final TelegramService telegramService;
    private final UserServiceClient userServiceClient;

    @KafkaListener(
            topics = "transaction.refunded",
            groupId = "notification-service",
            containerFactory = "transactionRefundedKafkaListenerContainerFactory"
    )
    public void handleRefunded(TransactionRefundedEvent event, Acknowledgment ack) {
        log.info("Received TransactionRefundedEvent for transaction: {}", event.getTransactionReference());
        try {
            Long userId = event.getReceiverUserId() != null ? event.getReceiverUserId() : event.getSenderUserId();
            UserServiceClient.UserNotificationInfo info = userServiceClient.getUserNotificationInfo(userId);
            String email = (info != null) ? info.getEmail() : null;
            if (email != null) {
                String amount = event.getRefundAmount() != null ? event.getRefundAmount().toString() : event.getTotalRefundAmount() != null ? event.getTotalRefundAmount().toString() : "0";
                String subject = "Remboursement de transaction Zaphira";
                String body = String.format(
                    "Bonjour,\n\n" +
                    "Un remboursement a été effectué pour votre transaction.\n\n" +
                    "Référence de transaction: %s\n" +
                    "Référence de remboursement: %s\n" +
                    "Montant remboursé: %s %s\n" +
                    "Raison: %s\n\n" +
                    "L'équipe Zaphira",
                    event.getTransactionReference(),
                    event.getRefundTransactionReference(),
                    amount, event.getCurrency(),
                    event.getReason()
                );
                emailService.sendEmail(email, subject, body);
                log.info("Refund email sent to user: {}", userId);
            }
            if (ack != null) ack.acknowledge();
        } catch (Exception e) {
            log.error("Error handling refunded event: {}", e.getMessage(), e);
            throw e;
        }
    }

    @KafkaListener(
            topics = "transaction.reversed",
            groupId = "notification-service",
            containerFactory = "transactionReversedKafkaListenerContainerFactory"
    )
    public void handleReversed(TransactionReversedEvent event, Acknowledgment ack) {
        log.info("Received TransactionReversedEvent for transaction: {}", event.getTransactionReference());
        try {
            Long userId = event.getSenderUserId() != null ? event.getSenderUserId() : event.getReceiverUserId();
            UserServiceClient.UserNotificationInfo info = userServiceClient.getUserNotificationInfo(userId);
            String email = (info != null) ? info.getEmail() : null;
            if (email != null) {
                String amount = event.getAmount() != null ? event.getAmount().toString() : "0";
                String subject = "Annulation de transaction Zaphira";
                String body = String.format(
                    "Bonjour,\n\n" +
                    "Votre transaction a été annulée.\n\n" +
                    "Référence de transaction: %s\n" +
                    "Référence d'annulation: %s\n" +
                    "Montant retourné: %s %s\n" +
                    "Raison: %s\n\n" +
                    "L'équipe Zaphira",
                    event.getTransactionReference(),
                    event.getReversalTransactionReference(),
                    amount, event.getCurrency(),
                    event.getReason()
                );
                emailService.sendEmail(email, subject, body);
                log.info("Reversal email sent to user: {}", userId);
            }
            if (ack != null) ack.acknowledge();
        } catch (Exception e) {
            log.error("Error handling reversed event: {}", e.getMessage(), e);
            throw e;
        }
    }

    @KafkaListener(
            topics = "transaction.validation.result",
            groupId = "notification-service-validation-result",
            containerFactory = "validationResultKafkaListenerContainerFactory"
    )
    public void handleValidationResult(TransactionValidationResult event, Acknowledgment ack) {
        log.info("Received TransactionValidationResult for transaction: {} status: {}", event.getTransactionId(), event.getValidationStatus());
        try {
            Long userId = event.getTransactionId(); // best-effort; if transactionId corresponds to user in your model, else consider fetching transaction owner
            UserServiceClient.UserNotificationInfo info = userServiceClient.getUserNotificationInfo(userId);
            String email = (info != null) ? info.getEmail() : null;
            if (email != null) {
                String status = event.getValidationStatus() != null ? event.getValidationStatus().name() : "UNKNOWN";
                String subject = "Résultat de validation de transaction Zaphira";
                String body = String.format(
                    "Bonjour,\n\n" +
                    "Résultat de validation pour votre transaction.\n\n" +
                    "ID de transaction: %s\n" +
                    "Statut de validation: %s\n" +
                    "Détail: %s\n\n" +
                    "L'équipe Zaphira",
                    event.getTransactionId(),
                    status,
                    event.getReason() != null ? event.getReason() : ""
                );
                emailService.sendEmail(email, subject, body);
                log.info("Validation result email sent to user: {}", userId);
            }
            if (ack != null) ack.acknowledge();
        } catch (Exception e) {
            log.error("Error handling validation result: {}", e.getMessage(), e);
            throw e;
        }
    }
}
