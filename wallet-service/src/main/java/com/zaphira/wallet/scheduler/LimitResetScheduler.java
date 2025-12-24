package com.zaphira.wallet.scheduler;

import com.zaphira.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LimitResetScheduler {

    private final WalletService walletService;

    /**
     * Réinitialise les limites journalières tous les jours à minuit
     */
    @Scheduled(cron = "0 0 0 * * *") // À minuit tous les jours
    public void resetDailyLimits() {
        log.info("Starting daily limits reset scheduled task");
        try {
            walletService.resetDailyLimits();
            log.info("Daily limits reset completed successfully");
        } catch (Exception e) {
            log.error("Error during daily limits reset", e);
        }
    }

    /**
     * Réinitialise les limites mensuelles le 1er de chaque mois à minuit
     */
    @Scheduled(cron = "0 0 0 1 * *") // À minuit le 1er de chaque mois
    public void resetMonthlyLimits() {
        log.info("Starting monthly limits reset scheduled task");
        try {
            walletService.resetMonthlyLimits();
            log.info("Monthly limits reset completed successfully");
        } catch (Exception e) {
            log.error("Error during monthly limits reset", e);
        }
    }
}