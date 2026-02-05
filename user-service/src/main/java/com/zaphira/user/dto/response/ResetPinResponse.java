package com.zaphira.user.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResetPinResponse {
    private boolean success;
    private String message;
}
