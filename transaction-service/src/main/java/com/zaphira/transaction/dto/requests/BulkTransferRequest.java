package com.zaphira.transaction.dto.requests;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkTransferRequest {

    @NotBlank
    private String senderWalletNumber;

    @Valid
    @NotEmpty
    private List<BulkTransferItemRequest> items;

    private String description;
}
