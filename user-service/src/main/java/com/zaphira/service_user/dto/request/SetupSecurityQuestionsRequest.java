package com.zaphira.service_user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class SetupSecurityQuestionsRequest {

    @NotEmpty(message = "Vous devez répondre à au moins 2 questions")
    @Size(min = 2, max = 3, message = "Choisissez entre 2 et 3 questions")
    private List<SecurityAnswerSetup> answers;

    @Data
    public static class SecurityAnswerSetup {
        @NotNull(message = "L'ID de la question est requis")
        private Long questionId;

        @NotBlank(message = "La réponse est requise")
        @Size(min = 2, max = 100, message = "La réponse doit contenir entre 2 et 100 caractères")
        private String answer;
    }
}