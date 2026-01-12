package com.zaphira.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VerifyOtpRequest {
    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "OTP code is required")
    private String code;
}