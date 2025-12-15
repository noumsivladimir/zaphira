package com.zaphira.auth.dto;

import lombok.Data;

@Data
public class UserResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String role;
    private String walletId;

    // Constructeur personnalisé
    public UserResponse(Long id, String firstName, String lastName, String role, String walletId) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.walletId = walletId;
    }

    // Optionnel : constructeur par défaut
    public UserResponse() {}
}
