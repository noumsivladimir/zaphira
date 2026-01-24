package com.zaphira.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?2376\\d{8}$", message = "Invalid phone number format")
    private String phoneNumber;

    @NotBlank(message = "PIN is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "PIN must be exactly 6 digits")
    private String pin;
}
