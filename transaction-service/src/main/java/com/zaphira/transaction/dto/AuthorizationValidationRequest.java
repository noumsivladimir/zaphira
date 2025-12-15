package com.zaphira.transaction.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.zaphira.transaction.model.enums.AuthorizationMethod;
import com.zaphira.transaction.util.CaseInsensitiveEnumDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Request DTO pour la validation de l'autorisation d'une transaction
 * Paramètres obligatoires: method, phoneNumber, otpCode
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthorizationValidationRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Authorization method (OTP, BIOMETRIC, PASSWORD)
     * Required for all transaction authorizations
     */
    @NotNull(message = "Authorization method is required")
    @JsonProperty("method")
    @JsonDeserialize(using = CaseInsensitiveEnumDeserializer.class)
    private AuthorizationMethod method;

    /**
     * Phone number associated with the OTP verification
     * Required for OTP-based authorization
     * Format: +33612345678 or 0612345678
     */
    @NotBlank(message = "Phone number is required for authorization")
    @JsonProperty("phoneNumber")
    private String phoneNumber;

    /**
     * OTP code sent to the user's phone
     * Required for OTP method authorization
     * Length: 6 digits
     */
    @NotBlank(message = "OTP code is required for transaction authorization")
    @JsonProperty("otpCode")
    private String otpCode;
}


