package com.zaphira.service_user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifyPinRequest {

    @NotBlank(message = "Current pin is required")
    @Pattern(
            regexp = "^[0-9]{6}$",
            message = "PIN must be exactly 6 digits"
    )
    private String oldPin;
}
