package com.zaphira.transaction.dto;

import com.zaphira.transaction.model.enums.AuthorizationMethod;
import com.zaphira.transaction.model.enums.AuthorizationStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AuthorizationInfoResponse {
    private Long authorizationId;
    private AuthorizationStatus status;
    private AuthorizationMethod method;
    private LocalDateTime requestedAt;
    private LocalDateTime expiresAt;
    private boolean challengeExposed;
    private String challengeCode;
}


