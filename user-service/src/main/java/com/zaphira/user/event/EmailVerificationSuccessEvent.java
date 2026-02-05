package com.zaphira.user.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Événement déclenché lorsqu'un email est vérifié avec succès
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationSuccessEvent {
    private Long userId;
    private String email;
    private String verificationCode;
}
