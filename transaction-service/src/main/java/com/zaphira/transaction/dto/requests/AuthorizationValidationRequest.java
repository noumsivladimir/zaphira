package com.zaphira.transaction.dto.requests;

import com.zaphira.transaction.model.enums.AuthorizationMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthorizationValidationRequest {

    @NotNull
    private AuthorizationMethod method;

    @NotBlank
    private String code;

    @NotBlank
    private String authorizedBy;
}


