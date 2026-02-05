package com.zaphira.user.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VerifySecurityQuestionsResponse {
    private boolean success;
    private String message;
    private String resetToken;  // Nouveau token pour l'étape finale
}
