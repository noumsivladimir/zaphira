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
public class KYCRejectedEvent {
    private Long kycId;
    private Long userId;
    private String email;
    private String reason;
    private String rejectedBy;
    private LocalDateTime rejectedAt;
}