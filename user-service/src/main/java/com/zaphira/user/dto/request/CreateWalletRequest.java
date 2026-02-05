package com.zaphira.user.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class CreateWalletRequest {

    @NotNull(message = "User ID is required")
    private Long userId;
}
