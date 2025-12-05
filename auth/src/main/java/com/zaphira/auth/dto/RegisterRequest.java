package com.zaphira.auth.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String fullName;
    private String email;
    private String phoneNumber;
    private String pin; // champ PIN ajouté
}
