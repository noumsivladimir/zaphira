package com.zaphira.transaction.service.routing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Result of routing validation for a transaction.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoutingValidationResult {
    private boolean valid;
    private String message;
    private String errorCode;

    public static RoutingValidationResult success() {
        return RoutingValidationResult.builder()
                .valid(true)
                .message("Routing validation passed")
                .build();
    }

    public static RoutingValidationResult failure(String errorCode, String message) {
        return RoutingValidationResult.builder()
                .valid(false)
                .errorCode(errorCode)
                .message(message)
                .build();
    }
}
