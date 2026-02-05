package com.zaphira.user.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KYCSubmittedEvent {
    private Long kycId;
    private Long userId;
    private String email;
    private LocalDateTime submittedAt;
}
