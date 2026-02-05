package com.zaphira.user.dto.request;





import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class InitiatePinResetRequest {

    @NotBlank(message = "Le numéro de wallet est requis")
    private String walletId;

    @NotBlank(message = "Le numéro de téléphone est requis")
    @Pattern(regexp = "\\+237[0-9]{9}", message = "Format: +237XXXXXXXXX")
    private String phoneNumber;
}
