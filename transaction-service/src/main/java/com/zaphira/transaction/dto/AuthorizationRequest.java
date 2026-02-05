package com.zaphira.transaction.dto;

import com.zaphira.transaction.model.entities.Transaction;
import com.zaphira.transaction.model.enums.AuthorizationMethod;
import com.zaphira.transaction.model.enums.AuthorizationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthorizationRequest {
    private Long id;
    private Transaction transaction;
    private AuthorizationMethod method;
    private AuthorizationStatus status;
    private String requestedBy;
    private String approvedBy;
    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime expiresAt;
    private String challengeCode;
    private String rejectionReason;
}
