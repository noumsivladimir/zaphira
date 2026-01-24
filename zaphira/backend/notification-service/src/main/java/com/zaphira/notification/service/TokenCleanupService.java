package com.zaphira.notification.service;

import com.zaphira.notification.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupService {

    private final VerificationTokenRepository tokenRepository;

    /**
     * Nettoie automatiquement les tokens expirés toutes les 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes en millisecondes
    @Transactional
    public void cleanupExpiredTokens() {
        try {
            LocalDateTime now = LocalDateTime.now();
            int deletedCount = tokenRepository.deleteExpiredTokens(now);

            if (deletedCount > 0) {
                log.info("🧹 Nettoyage automatique: {} tokens expirés supprimés", deletedCount);
            }
        } catch (Exception e) {
            log.error("❌ Erreur lors du nettoyage automatique des tokens: {}", e.getMessage());
        }
    }

    /**
     * Nettoie également les tokens utilisés de plus de 24h (nettoyage profond)
     */
    @Scheduled(fixedRate = 3600000) // 1 heure
    @Transactional
    public void cleanupOldUsedTokens() {
        try {
            LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
            int deletedCount = tokenRepository.deleteOldUsedTokens(oneDayAgo);

            if (deletedCount > 0) {
                log.info("🧹 Nettoyage profond: {} tokens utilisés anciens supprimés", deletedCount);
            }
        } catch (Exception e) {
            log.error("❌ Erreur lors du nettoyage profond des tokens: {}", e.getMessage());
        }
    }
}