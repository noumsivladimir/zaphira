package com.zaphira.auth.dto;

import com.zaphira.common.dto.WalletDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResponse {
    private String accessToken;
    private String refreshToken;
    private UserResponse user;
    private WalletDTO wallet; // Wallet créé automatiquement
}

