package com.zaphira.transaction.service.limit;

import com.zaphira.transaction.config.LimitProperties;
import com.zaphira.transaction.dto.TransactionRequest;
import com.zaphira.transaction.integration.wallet.FeignWalletClient;
import com.zaphira.common.dto.WalletDTO;
import com.zaphira.transaction.model.enums.AuthorizationMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class TransactionLimitService {

    private final LimitProperties properties;
    private final FeignWalletClient walletClient;

    public void validateLimits(TransactionRequest request) {
        BigDecimal amount = request.getAmount();

        // Vérifier la limite par transaction
        if (amount.compareTo(properties.getPerTransaction()) > 0) {
            throw new IllegalArgumentException("Amount " + amount + " exceeds per transaction limit of " + properties.getPerTransaction());
        }

        // Récupérer les informations du wallet expéditeur
        try {
            WalletDTO senderWallet = walletClient.getWalletByNumber(request.getSenderWalletNumber());

            // Vérifier les limites quotidiennes
            if (senderWallet.getDailyLimit() != null) {
                BigDecimal newDailySpent = senderWallet.getDailySpent() != null ?
                    senderWallet.getDailySpent().add(amount) : amount;
                if (newDailySpent.compareTo(senderWallet.getDailyLimit()) > 0) {
                    throw new IllegalArgumentException("Transaction would exceed daily limit of " + senderWallet.getDailyLimit());
                }
            }

            // Vérifier les limites mensuelles
            if (senderWallet.getMonthlyLimit() != null) {
                BigDecimal newMonthlySpent = senderWallet.getMonthlySpent() != null ?
                    senderWallet.getMonthlySpent().add(amount) : amount;
                if (newMonthlySpent.compareTo(senderWallet.getMonthlyLimit()) > 0) {
                    throw new IllegalArgumentException("Transaction would exceed monthly limit of " + senderWallet.getMonthlyLimit());
                }
            }

            // Vérifier si le wallet est gelé
            if (senderWallet.getFrozenAt() != null) {
                throw new IllegalArgumentException("Sender wallet is frozen and cannot perform transactions");
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to validate wallet limits: " + e.getMessage(), e);
        }
    }

    public LimitEvaluationResult evaluateAuthorizationNeed(TransactionRequest request) {
        boolean needsAuthorization = request.getAmount().compareTo(properties.getAuthorizationThreshold()) >= 0;
        if (!needsAuthorization) {
            return LimitEvaluationResult.builder()
                    .authorizationRequired(false)
                    .reason("Amount below threshold")
                    .build();
        }

        AuthorizationMethod method = switch (properties.getAuthorization().getDefaultMode()) {
            case PIN -> AuthorizationMethod.PIN;
            case ADMIN -> AuthorizationMethod.ADMIN;
            default -> AuthorizationMethod.OTP;
        };

        return LimitEvaluationResult.builder()
                .authorizationRequired(true)
                .method(method)
                .reason("Amount above authorization threshold")
                .build();
    }
}


