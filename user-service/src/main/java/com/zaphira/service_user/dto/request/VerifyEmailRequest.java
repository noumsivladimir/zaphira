package com.zaphira.service_user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VerifyEmailRequest {

    @NotBlank(message = "L'adresse email est requise")
    @Email(message = "L'adresse email doit être valide")
    private String email;

    @NotBlank(message = "Le code de vérification est requis")
    @Pattern(regexp = "\\d{6}", message = "Le code de vérification doit contenir 6 chiffres")
    private String verificationCode;
}