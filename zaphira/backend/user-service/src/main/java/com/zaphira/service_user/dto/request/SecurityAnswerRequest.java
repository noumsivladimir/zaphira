package com.zaphira.service_user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SecurityAnswerRequest {
    @NotNull(message = "L'ID de la question est requis")
    private Long questionId;

    @NotBlank(message = "La réponse est requise")
    @Size(min = 2, max = 100, message = "La réponse doit contenir entre 2 et 100 caractères")
    private String answer;
}
