package com.zaphira.service_user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VerifyPinResponse {
    private boolean success;
    private String message;
    private Integer remainingAttempts;
    private Boolean accountLocked;
}
