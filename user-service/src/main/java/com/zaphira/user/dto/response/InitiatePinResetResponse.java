package com.zaphira.user.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InitiatePinResetResponse {
    private boolean success;
    private String message;
    private String maskedPhoneNumber;  // Ex: +237****5678
}
