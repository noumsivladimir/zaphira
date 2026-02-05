package com.zaphira.user.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class VerifySecurityQuestionsRequest {

    @NotBlank(message = "Le token de reset est requis")
    private String resetToken;

    @NotEmpty(message = "Les réponses sont requises")
    private List<SecurityAnswerDto> answers;

    @Data
    public static class SecurityAnswerDto {
        private Long questionId;
        private String answer;
    }
}
