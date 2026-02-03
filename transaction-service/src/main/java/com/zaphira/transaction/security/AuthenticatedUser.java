package com.zaphira.transaction.security;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class AuthenticatedUser {
    private Long id;
    private String email;
    private List<String> roles;
}
