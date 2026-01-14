package com.zaphira.notification.listener;

import com.zaphira.common.event.UserRegisteredEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("no-kafka")
public class NoOpUserEventListener implements EventListener {

    /**
     * No-op implementation when Kafka is not available
     */
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("Kafka not available - skipping user registration event processing for user: {}", event.getUserId());
    }
}