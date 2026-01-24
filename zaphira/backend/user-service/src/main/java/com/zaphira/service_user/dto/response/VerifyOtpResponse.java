package com.zaphira.service_user.dto.response;

import com.zaphira.service_user.model.entities.PredefinedSecurityQuestion;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class VerifyOtpResponse {
    private boolean success;
    private String message;
    private String resetToken;  // Token temporaire pour l'étape suivante
    private List<SecurityQuestionDto> questions;

    @Data
    @Builder
    public static class SecurityQuestionDto {
        private Long id;
        private PredefinedSecurityQuestion question;
    }
}