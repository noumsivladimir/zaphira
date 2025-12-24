package com.zaphira.common.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FreezeWalletRequest {

    @NotBlank(message = "Reason is required")
    private String reason;

    @NotBlank(message = "Frozen by is required")
    private String frozenBy;

    private String notes;
}
