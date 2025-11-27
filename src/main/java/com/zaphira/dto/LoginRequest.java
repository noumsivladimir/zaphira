package com.zaphira.zaphira;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
}
