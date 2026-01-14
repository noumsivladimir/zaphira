package com.zaphira.transaction.kafka.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionValidationResult {

    private String correlationId;
    private Long transactionId;
    @JsonProperty("validationStatus")
    private ValidationStatus validationStatus;
    private String reason;
    private String validatorId;
    private LocalDateTime validatedAt;
    private Integer riskScoreFinal;
    @JsonProperty("complianceStatus")
    private ComplianceStatus complianceStatus;
    private Map<String, Object> metadata;
    @Builder.Default
    private Integer schemaVersion = 1;

    public enum ValidationStatus {
        APPROVED, REJECTED, EXPIRED
    }

    public enum ComplianceStatus {
        CLEAR, FLAGGED, BLOCKED
    }
}
