package com.zaphira.service_user.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRegisteredEvent {
    private Long userId;
    private String email;
    private String firstName;
    private String lastName;
    private String roleType;
    private LocalDateTime registeredAt;
}