// kafka/WalletResponseListener.java
package com.zaphira.service_user.kafka;

import com.zaphira.common.event.WalletCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class WalletResponseListener {

    private final Map<String, CompletableFuture<WalletCreatedEvent>> pendingRequests = new ConcurrentHashMap<>();

    public CompletableFuture<WalletCreatedEvent> createPendingRequest(String correlationId) {
        CompletableFuture<WalletCreatedEvent> future = new CompletableFuture<>();
        pendingRequests.put(correlationId, future);

        // Auto-cleanup après timeout
        future.orTimeout(30, TimeUnit.SECONDS)
                .whenComplete((result, ex) -> pendingRequests.remove(correlationId));

        return future;
    }

    @KafkaListener(topics = "${kafka.topics.wallet-created}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleWalletCreatedEvent(WalletCreatedEvent event) {
        log.info("Received wallet created event: {}", event);

        CompletableFuture<WalletCreatedEvent> future = pendingRequests.remove(event.getCorrelationId());
        if (future != null) {
            future.complete(event);
        } else {
            log.warn("No pending request found for correlationId: {}", event.getCorrelationId());
        }
    }
}