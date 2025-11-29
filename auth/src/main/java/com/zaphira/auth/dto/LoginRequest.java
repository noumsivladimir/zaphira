package com.zaphira.auth.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String emailOrPhone; // <-- nouveau champ
    private String password;
}
