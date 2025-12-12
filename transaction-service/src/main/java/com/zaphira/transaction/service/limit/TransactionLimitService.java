package com.zaphira.transaction.service.limit;

import com.zaphira.transaction.config.LimitProperties;
import com.zaphira.transaction.dto.TransactionRequest;
import com.zaphira.transaction.model.enums.AuthorizationMethod;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class TransactionLimitService {

    private final LimitProperties properties;

    public TransactionLimitService(LimitProperties properties) {
        this.properties = properties;
    }

    public void validateLimits(TransactionRequest request) {
        BigDecimal amount = request.getAmount();
        if (amount.compareTo(properties.getPerTransaction()) > 0) {
            throw new IllegalArgumentException("Amount exceeds per transaction limit");
        }
        // TODO: implement daily/weekly/monthly aggregation once wallet ledger is available.
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


