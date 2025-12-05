package com.zaphira.auth.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String phoneNumber; // ancien email
    private String pin;         // ancien password
}
