package com.zaphira.user.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "notification-service")
public interface NotificationServiceClient {

    @PostMapping("/api/notifications/send/otp/{userId}")
    ResponseEntity<Map<String, Object>> sendOtp(@PathVariable Long userId);

    @PostMapping("/api/notifications/send/otp")
    ResponseEntity<Map<String, Object>> sendOtpPayload(@RequestBody Map<String, Object> payload);

    @PostMapping("/api/notifications/verify/otp")
    ResponseEntity<Map<String, Object>> verifyOtp(@RequestBody Map<String, Object> request);
}
