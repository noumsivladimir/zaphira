package com.zaphira.transaction.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionValidationResponse {

    private Boolean isValid;

    private String message;

    private String errorCode;

    // Détails de validation
    private Boolean walletActive;

    private Boolean sufficientBalance;

    private Boolean withinLimits;

    private Boolean kycCompliant;
}