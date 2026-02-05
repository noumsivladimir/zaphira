package com.zaphira.user.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SetupSecurityQuestionsResponse {
    private boolean success;
    private String message;
    private int questionsConfigured;
}
