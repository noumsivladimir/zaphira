package com.zaphira.user.controller;

import com.zaphira.user.dto.request.SetupSecurityQuestionsRequest;
import com.zaphira.user.dto.response.SecurityQuestionsListResponse;
import com.zaphira.user.dto.response.SetupSecurityQuestionsResponse;
import com.zaphira.user.service.SecurityQuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/security-questions")
@RequiredArgsConstructor
public class SecurityQuestionController {

    private final SecurityQuestionService securityQuestionService;

    // Lister les questions disponibles
    @GetMapping
    public ResponseEntity<SecurityQuestionsListResponse> getAvailableQuestions() {
        return ResponseEntity.ok(securityQuestionService.getAvailableQuestions());
    }

    // Configurer les questions de sécurité
    @PostMapping("/setup/{userId}")
    public ResponseEntity<SetupSecurityQuestionsResponse> setupSecurityQuestions(
            @PathVariable Long userId,
            @Valid @RequestBody SetupSecurityQuestionsRequest request) {
        return ResponseEntity.ok(securityQuestionService.setupSecurityQuestions(userId, request));
    }

     //Vérifier si l'utilisateur a configuré ses questions
    @GetMapping("/status/{userId}")
    public ResponseEntity<Map<String, Boolean>> checkStatus(@PathVariable Long userId) {
        boolean configured = securityQuestionService.hasConfiguredQuestions(userId);
        return ResponseEntity.ok(Map.of("configured", configured));
    }
}
