package com.zaphira.wallet.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

//    private final WalletServiceImpl walletService;
//    private final KafkaTemplate<String, WalletCreatedEvent> kafkaTemplate;
//
//@Value("${kafka.topics.wallet-created}")
//    private String walletCreatedTopic;
//
//    @KafkaListener(topics = "${kafka.topics.user-created}", groupId = "${spring.kafka.consumer.group-id}")
//    public void handleUserCreatedEvent(UserCreatedEvent event) {
//        log.info("Received user created event: {}", event);
//
//        WalletCreatedEvent response;
//        try {
//            WalletDTO wallet = walletService.createWallet(event.getUserId());
//            response = WalletCreatedEvent.builder()
//                    .walletId(wallet.getWalletNumber())
//                    .userId(event.getUserId())
//                    .correlationId(event.getCorrelationId())
//                    .success(true)
//                    .build();
//            log.info("Wallet created successfully for user: {}", event.getUserId());
//        } catch (Exception e) {
//            log.error("Failed to create wallet for user: {}", event.getUserId(), e);
//            response = WalletCreatedEvent.builder()
//                    .userId(event.getUserId())
//                    .correlationId(event.getCorrelationId())
//                    .success(false)
//                    .errorMessage(e.getMessage())
//                    .build();
//        }
//
//        kafkaTemplate.send(walletCreatedTopic, event.getCorrelationId(), response);
//    }
}