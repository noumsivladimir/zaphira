package com.zaphira.wallet.dto.response;

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

    public static TransactionValidationResponse valid() {
        return TransactionValidationResponse.builder()
                .isValid(true)
                .message("Transaction autorisée")
                .build();
    }

    public static TransactionValidationResponse invalid(String message, String errorCode) {
        return TransactionValidationResponse.builder()
                .isValid(false)
                .message(message)
                .errorCode(errorCode)
                .build();
    }
}