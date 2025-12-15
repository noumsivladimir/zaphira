package com.zaphira.transaction.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.zaphira.transaction.model.enums.TransactionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Request DTO pour la mise à jour du statut d'une transaction
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateStatusRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "Status is required")
    @JsonProperty("status")
    private TransactionStatus status;

    @NotBlank(message = "ChangedBy is required")
    @JsonProperty("changedBy")
    private String changedBy;

    @JsonProperty("reason")
    private String reason;
}



