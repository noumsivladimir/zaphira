package com.zaphira.notification.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Map;

@FeignClient(name = "user-service")
public interface UserServiceFeignClient {

    @PostMapping("/api/users/{userId}/generate-otp")
    ResponseEntity<Map<String, Object>> generateOtpForUser(@PathVariable Long userId);
}