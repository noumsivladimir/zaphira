package com.zaphira.transaction.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for OTP authorization information
 * Indicates if OTP is required and provides details for the OTP flow
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpAuthorizationResponse {

    @JsonProperty("transactionId")
    private Long transactionId;

    @JsonProperty("otpRequired")
    private Boolean otpRequired;

    @JsonProperty("phoneNumber")
    private String phoneNumber;

    @JsonProperty("otpExpiry")
    private LocalDateTime otpExpiry;

    @JsonProperty("attemptsRemaining")
    private Integer attemptsRemaining;

    @JsonProperty("message")
    private String message;

    @JsonProperty("status")
    private String status;
}
