package com.zaphira.notification.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserVerificationStatusResponse {
    private boolean verified;
    private String status;
    private String email;
}