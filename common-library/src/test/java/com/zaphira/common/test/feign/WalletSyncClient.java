package com.zaphira.common.test.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for synchronous test messaging with wallet service.
 * Replaces Kafka for the 'test-sync' profile.
 */
@Profile("test-sync")
@FeignClient(
    name = "wallet-service-sync-test",
    url = "${test.sync.wallet-service.url:http://localhost:8082}",
    fallback = WalletSyncClient.Fallback.class
)
public interface WalletSyncClient {
    
    @PostMapping("/test-sync/messages/{topic}")
    void sendMessage(
        @PathVariable(name = "topic") String topic,
        @RequestBody Object payload
    );

    class Fallback implements WalletSyncClient {
        @Override
        public void sendMessage(String topic, Object payload) {
            throw new RuntimeException(
                "wallet-service-sync-test is not available. " +
                "Ensure the service is running with @ActiveProfiles(\"test-sync\") " +
                "and test.sync.wallet-service.url is configured."
            );
        }
    }
}
