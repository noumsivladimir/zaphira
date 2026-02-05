// kafka/UserEventProducer.java
package com.zaphira.user.kafka;

import com.zaphira.common.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventProducer {

    private final KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate;

    @Value("${kafka.topics.user-created}")
    private String userCreatedTopic;

    public void publishUserCreatedEvent(UserRegisteredEvent event) {
        log.info("Publishing user created event: {}", event);
        kafkaTemplate.send(userCreatedTopic, event.getUserId().toString(), event);
    }
}
