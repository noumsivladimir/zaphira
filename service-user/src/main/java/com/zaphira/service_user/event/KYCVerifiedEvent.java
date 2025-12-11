package com.zaphira.service_user.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KYCVerifiedEvent {
    private Long kycId;
    private Long userId;
    private String email;
    private Long verifiedBy;
    private LocalDateTime verifiedAt;
}