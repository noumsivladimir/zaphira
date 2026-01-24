package com.zaphira.service_user.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VerifyOtpRequest {

    @NotBlank(message = "Le numéro de téléphone est requis")
    private String phoneNumber;

    @NotBlank(message = "Le code OTP est requis")
    @Pattern(regexp = "\\d{6}", message = "Le code OTP doit contenir 6 chiffres")
    private String otpCode;
}