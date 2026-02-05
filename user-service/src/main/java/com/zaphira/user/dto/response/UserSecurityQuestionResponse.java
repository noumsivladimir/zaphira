package com.zaphira.user.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;


@Data
@Builder
public class UserSecurityQuestionResponse {

    private String walletId;
    private Long questionId;
    private String question;
    private LocalDateTime createdAt;
}
