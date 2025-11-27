package com.zaphira.zaphira;

import lombok.Data;

@Data
public class RegisterRequest {
    private String email;
    private String fullName;
    private String password;
}
