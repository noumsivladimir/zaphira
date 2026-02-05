package com.zaphira.user.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SecurityQuestionsListResponse {
    private List<QuestionDto> questions;

    @Data
    @Builder
    public static class QuestionDto {
        private Long id;
        private String
                question;
    }
}
