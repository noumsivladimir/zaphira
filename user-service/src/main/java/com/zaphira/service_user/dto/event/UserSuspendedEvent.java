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
public class UserSuspendedEvent {
    private Long userId;
    private String email;
    private String reason;
    private LocalDateTime suspendedAt;
}