package com.zaphira.notification.listener;

import com.zaphira.common.event.TransactionCreatedEvent;
//import com.zaphira.notification.service.EmailService;
//import com.zaphira.notification.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventListener {
    
    //private final EmailService emailService;
    //private final SmsService smsService;

    @KafkaListener(
            topics = "transaction-created",
            groupId = "notification-service",
            containerFactory = "transactionKafkaListenerContainerFactory"
    )
    public void handleTransactionCreated(TransactionCreatedEvent event) {
        log.info("Received transaction event: {}", event.getReference());
        
        try {
            // Send email notification (if email available)
            // emailService.sendTransactionNotification(
            //     userEmail,
            //     event.getReference(),
            //     event.getAmount().toString(),
            //     event.getStatus()
            // );
            
            // Send SMS notification (if phone number available)
            // smsService.sendTransactionSms(
            //     userPhoneNumber,
            //     event.getReference(),
            //     event.getAmount().toString(),
            //     event.getStatus()
            // );
            
            log.info("Transaction notification processed for: {}", event.getReference());
        } catch (Exception e) {
            log.error("Failed to process transaction notification for: {}", event.getReference(), e);
        }
    }
}

