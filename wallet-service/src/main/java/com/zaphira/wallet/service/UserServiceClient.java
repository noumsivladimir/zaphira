package com.zaphira.wallet.service;

import com.zaphira.wallet.dto.UserStatusDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", url = "${user.service.url:http://localhost:8082}")
public interface UserServiceClient {

    @GetMapping("/api/users/{userId}/status")
    UserStatusDTO getUserStatus(@PathVariable("userId") Long userId);

    @GetMapping("/api/users/{userId}/kyc-status")
    String getKycStatus(@PathVariable("userId") Long userId);
}
