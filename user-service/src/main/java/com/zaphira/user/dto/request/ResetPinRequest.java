package com.zaphira.user.dto.request;




import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ResetPinRequest {

    @NotBlank(message = "Le token de reset est requis")
    private String resetToken;

    @NotBlank(message = "Le nouveau PIN est requis")
    @Pattern(regexp = "\\d{4,6}", message = "Le PIN doit contenir 4 à 6 chiffres")
    private String newPin;

    @NotBlank(message = "La confirmation du PIN est requise")
    private String confirmPin;
}
