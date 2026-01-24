package com.zaphira.service_user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangePinRequest {

    @NotBlank(message = "Wallet number is required")
    @Size (min = 6, message = "Wallet number must be 6 digits")
    private String walletNumber;


    @NotBlank(message = "Current pin is required")
        @Pattern(
            regexp = "^[0-9]{6}$",
            message = "PIN must be exactly 6 digits"
        )
    private String oldPin;

    @NotBlank(message = "New pin is required")
    @Size(min = 6, message = "PIN must be exactly 6 digits")
    @Pattern(
            regexp = "^[0-9]{6}$",
            message = "PIN must be exactly 6 digits"
    )
    private String newPin;

    @NotBlank(message = "Pin confirmation is required")
    @Pattern(
            regexp = "^[0-9]{6}$",
            message = "PIN must be exactly 6 digits"
    )
    private String newPinConfirmation;
}
