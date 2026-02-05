package com.zaphira.user.mapper;

import com.zaphira.user.dto.response.SessionResponse;
import com.zaphira.user.model.entities.UserSession;
import org.springframework.stereotype.Component;

@Component
public class SessionMapper {

    public SessionResponse toResponse(UserSession session, Boolean isCurrent) {
        if (session == null) {
            return null;
        }

        return SessionResponse.builder()
                .sessionId(session.getSessionId())
                .deviceType(session.getDeviceType())
                .browser(session.getBrowser())
                .operatingSystem(session.getOperatingSystem())
                .ipAddress(session.getIpAddress())
                .location(session.getLocation())
                .loginTime(session.getLoginTime())
                .lastActivity(session.getExpiryTime())
                .isActive(session.getIsActive())
                .isCurrent(isCurrent)
                .build();
    }

//    public List<SessionResponse> toResponseList(List<UserSession> sessions, String currentToken) {
//        if (sessions == null || sessions.isEmpty()) {
//            return List.of();
//        }
//
//        return sessions.stream()
//                .map(session -> toResponse(session, session.getToken().equals(currentToken)))
//                .collect(Collectors.toList());
//    }
}
