package com.zaphira.service_user.dto.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventPublisher {

//    private final KafkaTemplate<String, Object> kafkaTemplate;
//
//    private static final String USER_EVENTS_TOPIC = "user-events";
//    private static final String KYC_EVENTS_TOPIC = "kyc-events";
//
//    public void publishUserRegisteredEvent(User user) {
//        try {
//            UserRegisteredEvent event = UserRegisteredEvent.builder()
//                    .userId(user.getUserId())
//                    .email(user.getEmail())
//                    .firstName(user.getFirstName())
//                    .lastName(user.getLastName())
//                    .roleType(user.getRoleType().name())
//                    .registeredAt(LocalDateTime.now())
//                    .build();
//
//            kafkaTemplate.send(USER_EVENTS_TOPIC, "user.registered", event);
//            log.info("Published UserRegisteredEvent for user ID: {}", user.getUserId());
//        } catch (Exception e) {
//            log.error("Failed to publish UserRegisteredEvent", e);
//        }
//    }
//
//    public void publishUserUpdatedEvent(User user) {
//        try {
//            UserUpdatedEvent event = UserUpdatedEvent.builder()
//                    .userId(user.getUserId())
//                    .email(user.getEmail())
//                    .updatedAt(LocalDateTime.now())
//                    .build();
//
//            kafkaTemplate.send(USER_EVENTS_TOPIC, "user.updated", event);
//            log.info("Published UserUpdatedEvent for user ID: {}", user.getUserId());
//        } catch (Exception e) {
//            log.error("Failed to publish UserUpdatedEvent", e);
//        }
//    }
//
//    public void publishUserSuspendedEvent(User user, String reason) {
//        try {
//            UserSuspendedEvent event = UserSuspendedEvent.builder()
//                    .userId(user.getUserId())
//                    .email(user.getEmail())
//                    .reason(reason)
//                    .suspendedAt(LocalDateTime.now())
//                    .build();
//
//            kafkaTemplate.send(USER_EVENTS_TOPIC, "user.suspended", event);
//            log.info("Published UserSuspendedEvent for user ID: {}", user.getUserId());
//        } catch (Exception e) {
//            log.error("Failed to publish UserSuspendedEvent", e);
//        }
//    }
//
//    public void publishKYCSubmittedEvent(KYC kyc) {
//        try {
//            KYCSubmittedEvent event = KYCSubmittedEvent.builder()
//                    .kycId(kyc.getKycId())
//                    .userId(kyc.getUser().getUserId())
//                    .email(kyc.getUser().getEmail())
//                    .submittedAt(LocalDateTime.now())
//                    .build();
//
//            kafkaTemplate.send(KYC_EVENTS_TOPIC, "kyc.submitted", event);
//            log.info("Published KYCSubmittedEvent for KYC ID: {}", kyc.getKycId());
//        } catch (Exception e) {
//            log.error("Failed to publish KYCSubmittedEvent", e);
//        }
//    }
//
//    public void publishKYCVerifiedEvent(KYC kyc) {
//        try {
//            KYCVerifiedEvent event = KYCVerifiedEvent.builder()
//                    .kycId(kyc.getKycId())
//                    .userId(kyc.getUser().getUserId())
//                    .email(kyc.getUser().getEmail())
//                    .verifiedBy(kyc.getVerifiedBy())
//                    .verifiedAt(LocalDateTime.now())
//                    .build();
//
//            kafkaTemplate.send(KYC_EVENTS_TOPIC, "kyc.verified", event);
//            log.info("Published KYCVerifiedEvent for KYC ID: {}", kyc.getKycId());
//        } catch (Exception e) {
//            log.error("Failed to publish KYCVerifiedEvent", e);
//        }
//    }
//
//    public void publishKYCRejectedEvent(KYC kyc, String reason) {
//        try {
//            KYCRejectedEvent event = KYCRejectedEvent.builder()
//                    .kycId(kyc.getKycId())
//                    .userId(kyc.getUser().getUserId())
//                    .email(kyc.getUser().getEmail())
//                    .reason(reason)
//                    .rejectedBy(kyc.getRejectedBy())
//                    .rejectedAt(LocalDateTime.now())
//                    .build();
//
//            kafkaTemplate.send(KYC_EVENTS_TOPIC, "kyc.rejected", event);
//            log.info("Published KYCRejectedEvent for KYC ID: {}", kyc.getKycId());
//        } catch (Exception e) {
//            log.error("Failed to publish KYCRejectedEvent", e);
//        }
//    }
}