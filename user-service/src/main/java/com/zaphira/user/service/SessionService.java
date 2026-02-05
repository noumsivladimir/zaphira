package com.zaphira.user.service;

import com.zaphira.user.dto.response.SessionResponse;
import com.zaphira.user.mapper.SessionMapper;
import com.zaphira.user.model.entities.UserSession;
import com.zaphira.user.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing user sessions
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final UserSessionRepository sessionRepository;
    private final SessionMapper sessionMapper;

    /**
     * Get all active sessions for a user
     */
    @Transactional(readOnly = true)
    public List<SessionResponse> getActiveSessions(Long userId, String currentToken) {
        log.info("Fetching active sessions for user: {}", userId);
        
        List<UserSession> sessions = sessionRepository.findByUserIdAndIsActive(userId, true);
        
        return sessions.stream()
                .map(session -> {
                    boolean isCurrent = session.getToken().equals(currentToken);
                    return sessionMapper.toResponse(session, isCurrent);
                })
                .collect(Collectors.toList());
    }

    /**
     * Terminate a specific session
     */
    @Transactional
    public void terminateSession(Long userId, Long sessionId) {
        log.info("Terminating session {} for user: {}", sessionId, userId);
        
        UserSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
        
        if (!session.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Session does not belong to user");
        }
        
        session.setIsActive(false);
        session.setLogoutTime(LocalDateTime.now());
        sessionRepository.save(session);
        
        log.info("Session {} terminated successfully", sessionId);
    }

    /**
     * Terminate all sessions for a user (logout from all devices)
     */
    @Transactional
    public void logoutAllDevices(Long userId) {
        log.info("Logging out user {} from all devices", userId);
        
        List<UserSession> activeSessions = sessionRepository.findByUserIdAndIsActive(userId, true);
        
        LocalDateTime now = LocalDateTime.now();
        activeSessions.forEach(session -> {
            session.setIsActive(false);
            session.setLogoutTime(now);
        });
        
        sessionRepository.saveAll(activeSessions);
        
        log.info("User {} logged out from {} devices", userId, activeSessions.size());
    }
}
