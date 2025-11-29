package com.zaphira.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String fullName;
    private String email;
    private String role;
    private Long walletId;
    private String phoneNumber; // <-- ajout
}
