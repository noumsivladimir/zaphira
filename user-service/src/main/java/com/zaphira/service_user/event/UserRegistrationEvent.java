package com.zaphira.service_user.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Événement déclenché lorsqu'un utilisateur s'inscrit
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationEvent {
    private Long userId;
    private String email;
    private String phoneNumber;
    private String firstName;
    private String lastName;
    private String verificationCode;
}