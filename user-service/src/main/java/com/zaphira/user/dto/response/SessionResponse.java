package com.zaphira.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionResponse {

    private Long sessionId;
    private String deviceType;
    private String browser;
    private String operatingSystem;
    private String ipAddress;
    private String location;
    private LocalDateTime loginTime;
    private LocalDateTime lastActivity;
    private Boolean isActive;
    private Boolean isCurrent;
}
