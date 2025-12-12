package com.zaphira.transaction.dto;

import com.zaphira.transaction.model.enums.TransactionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateStatusRequest {

    @NotNull
    private TransactionStatus status;

    @NotBlank
    private String changedBy;

    private String reason;
}



