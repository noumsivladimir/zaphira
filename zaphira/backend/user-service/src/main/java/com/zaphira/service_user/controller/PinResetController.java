package com.zaphira.service_user.controller;

import com.zaphira.service_user.dto.request.InitiatePinResetRequest;
import com.zaphira.service_user.dto.request.ResetPinRequest;
import com.zaphira.service_user.dto.request.VerifyOtpRequest;
import com.zaphira.service_user.dto.request.VerifySecurityQuestionsRequest;
import com.zaphira.service_user.dto.response.InitiatePinResetResponse;
import com.zaphira.service_user.dto.response.ResetPinResponse;
import com.zaphira.service_user.dto.response.VerifyOtpResponse;
import com.zaphira.service_user.dto.response.VerifySecurityQuestionsResponse;
import com.zaphira.service_user.services.PinResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pin-reset")
@RequiredArgsConstructor
public class PinResetController {

    private final PinResetService pinResetService;

    // Étape 1: Initier le reset (envoi OTP)
    @PostMapping("/initiate")
    public ResponseEntity<InitiatePinResetResponse> initiateReset(
            @Valid @RequestBody InitiatePinResetRequest request) {
        return ResponseEntity.ok(pinResetService.initiateReset(request));
    }

    // Étape 2: Vérifier OTP
    @PostMapping("/verify-otp")
    public ResponseEntity<VerifyOtpResponse> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(pinResetService.verifyOtp(request));
    }

    // Étape 3: Vérifier questions de sécurité
    @PostMapping("/verify-security-questions")
    public ResponseEntity<VerifySecurityQuestionsResponse> verifySecurityQuestions(
            @Valid @RequestBody VerifySecurityQuestionsRequest request) {
        return ResponseEntity.ok(pinResetService.verifySecurityQuestions(request));
    }

    // Étape 4: Réinitialiser le PIN
    @PostMapping("/reset")
    public ResponseEntity<ResetPinResponse> resetPin(
            @Valid @RequestBody ResetPinRequest request) {
        return ResponseEntity.ok(pinResetService.resetPin(request));
    }
}