package com.zaphira.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TwoFactorSetupResponse {

    private String secret;
    private String qrCodeUrl;
    private String[] backupCodes;
}
