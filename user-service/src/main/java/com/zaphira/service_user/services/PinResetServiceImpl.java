package com.zaphira.service_user.services;

import com.zaphira.service_user.dto.request.InitiatePinResetRequest;
import com.zaphira.service_user.dto.request.ResetPinRequest;
import com.zaphira.service_user.dto.request.VerifyOtpRequest;
import com.zaphira.service_user.dto.request.VerifySecurityQuestionsRequest;
import com.zaphira.service_user.dto.response.InitiatePinResetResponse;
import com.zaphira.service_user.dto.response.ResetPinResponse;
import com.zaphira.service_user.dto.response.VerifyOtpResponse;
import com.zaphira.service_user.dto.response.VerifySecurityQuestionsResponse;
import com.zaphira.service_user.model.entities.User;
import com.zaphira.service_user.model.entities.UserSecurityAnswer;
import com.zaphira.service_user.model.enums.OtpPurpose;
import com.zaphira.service_user.repository.UserRepository;
import com.zaphira.service_user.repository.UserSecurityAnswerRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Service
@RequiredArgsConstructor
@Slf4j
public class PinResetServiceImpl implements PinResetService{

    private final UserRepository userRepository;
    private final UserSecurityAnswerRepository userSecurityAnswerRepository;
    private final OtpService otpService;
    private final PinService pinService;


    private final Map<String, ResetTokenData> resetTokens = new ConcurrentHashMap<>();
    private final OtpServiceImpl otpServiceImpl;

    @Override
    @Transactional
    public InitiatePinResetResponse initiateReset(InitiatePinResetRequest request) {
        log.info("Initiating PIN reset for wallet: {}", request.getWalletId());

        // Vérifier que le wallet existe et appartient au numéro de téléphone
        User user = userRepository.findByWalletId(request.getWalletId())
                .orElse(null);

        if (user == null || !user.getPhoneNumber().equals(request.getPhoneNumber())) {
            return InitiatePinResetResponse.builder()
                    .success(false)
                    .message("Wallet ou numéro de téléphone invalide")
                    .build();
        }

        // Vérifier si le compte est verrouillé
        if (user.getAccountLockedUntil() != null && user.getAccountLockedUntil().isAfter(LocalDateTime.now())) {
            return InitiatePinResetResponse.builder()
                    .success(false)
                    .message("Compte verrouillé. Contactez le support.")
                    .build();
        }

        // Envoyer l'OTP
        otpServiceImpl.generateAndSendOtp(request.getPhoneNumber(), OtpPurpose.PIN_RESET);

        return InitiatePinResetResponse.builder()
                .success(true)
                .message("Code OTP envoyé par SMS")
                .maskedPhoneNumber(maskPhoneNumber(request.getPhoneNumber()))
                .build();
    }

    @Override
    @Transactional
    public VerifyOtpResponse verifyOtp(VerifyOtpRequest request) {
        log.info("Verifying OTP for phone: {}", maskPhoneNumber(request.getPhoneNumber()));

        boolean isValid = otpService.verifyOtp(request.getPhoneNumber(), request.getOtpCode(), OtpPurpose.PIN_RESET);

        if (!isValid) {
            return VerifyOtpResponse.builder()
                    .success(false)
                    .message("Code OTP invalide ou expiré")
                    .build();
        }

        // Récupérer l'utilisateur
        User user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElse(null);

        if (user == null) {
            return VerifyOtpResponse.builder()
                    .success(false)
                    .message("Utilisateur non trouvé")
                    .build();
        }

        String resetToken = generateResetToken();

        // Sauvegarder le token avec l'état
        resetTokens.put(resetToken, ResetTokenData.builder()
                .userId(user.getUserId())
                .phoneNumber(request.getPhoneNumber())
                .otpVerified(true)
                .securityQuestionsVerified(false)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build());

        // Récupérer les questions de sécurité

        //     List<UserSecurityQuestionResponse> findByUserIdWithQuestion(@Param("userId") Long userId);
        List<UserSecurityAnswer> questions = userSecurityAnswerRepository.findByUserIdWithQuestion(user.getUserId());

        List<VerifyOtpResponse.SecurityQuestionDto> questionDtos = questions.stream()
                .map(q -> VerifyOtpResponse.SecurityQuestionDto.builder()
                        .id(q.getQuestion().getId())
                        .question(q.getQuestion())

                        .build())
                .toList();

        return VerifyOtpResponse.builder()
                .success(true)
                .message("OTP vérifié. Veuillez répondre aux questions de sécurité.")
                .resetToken(resetToken)
                .questions(questionDtos)
                .build();
    }

    @Override
    public VerifySecurityQuestionsResponse verifySecurityQuestions(VerifySecurityQuestionsRequest request) {
        return null;
    }

//    @Override
//    @Transactional
//    public VerifySecurityQuestionsResponse verifySecurityQuestions(VerifySecurityQuestionsRequest request) {
//        log.info("Verifying security questions");
//
//        // Valider le token
//        ResetTokenData tokenData = resetTokens.get(request.getResetToken());
//
//        if (tokenData == null || tokenData.isExpired()) {
//            return VerifySecurityQuestionsResponse.builder()
//                    .success(false)
//                    .message("Token invalide ou expiré. Veuillez recommencer.")
//                    .build();
//        }
//
//        if (!tokenData.isOtpVerified()) {
//            return VerifySecurityQuestionsResponse.builder()
//                    .success(false)
//                    .message("Veuillez d'abord vérifier l'OTP")
//                    .build();
//        }
//
//        // Vérifier les réponses
//        List<UserSecurityAnswer> questions = userSecurityAnswerRepository.findByUserId(tokenData.getUserId());
//
//        int correctAnswers = 0;
//
//        for (VerifySecurityQuestionsRequest.SecurityAnswerDto answer : request.getAnswers()) {
//            SecurityQuestion question = questions.stream()
//                    .filter(q -> q.getUser().equals(answer.getQuestionId()))
//                    .findFirst()
//                    .orElse(null);
//
//            if (question != null && pinService.verifyPin(answer.getAnswer().toLowerCase().trim(), question.getAnswerHash())) {
//                correctAnswers++;
//            }
//        }
//
//        // Vérifier que toutes les réponses sont correctes
//        if (correctAnswers < questions.size()) {
//            return VerifySecurityQuestionsResponse.builder()
//                    .success(false)
//                    .message("Réponses incorrectes. Veuillez réessayer.")
//                    .build();
//        }
//
//        // Mettre à jour le token
//        tokenData.setSecurityQuestionsVerified(true);
//
//        // Générer un nouveau token pour l'étape finale
//        String newResetToken = generateResetToken();
//        resetTokens.remove(request.getResetToken());
//        resetTokens.put(newResetToken, tokenData);
//
//        return VerifySecurityQuestionsResponse.builder()
//                .success(true)
//                .message("Questions de sécurité validées. Vous pouvez maintenant réinitialiser votre PIN.")
//                .resetToken(newResetToken)
//                .build();
//    }

    @Override
    @Transactional
    public ResetPinResponse resetPin(ResetPinRequest request) {
        log.info("Resetting PIN");

        // Valider le token
        ResetTokenData tokenData = resetTokens.get(request.getResetToken());

        if (tokenData == null || tokenData.isExpired()) {
            return ResetPinResponse.builder()
                    .success(false)
                    .message("Token invalide ou expiré. Veuillez recommencer.")
                    .build();
        }

        if (!tokenData.isOtpVerified() || !tokenData.isSecurityQuestionsVerified()) {
            return ResetPinResponse.builder()
                    .success(false)
                    .message("Veuillez compléter toutes les étapes de vérification")
                    .build();
        }

        // Vérifier que les PINs correspondent
        if (!request.getNewPin().equals(request.getConfirmPin())) {
            return ResetPinResponse.builder()
                    .success(false)
                    .message("Les PINs ne correspondent pas")
                    .build();
        }

        // Mettre à jour le PIN
        User user = userRepository.findById(tokenData.getUserId())
                .orElse(null);

        if (user == null) {
            return ResetPinResponse.builder()
                    .success(false)
                    .message("Utilisateur non trouvé")
                    .build();
        }

        user.setPin(pinService.hashPin(request.getNewPin()));
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        userRepository.save(user);

        // Supprimer le token
        resetTokens.remove(request.getResetToken());

        log.info("PIN reset successfully for user: {}", user.getWalletId());

        return ResetPinResponse.builder()
                .success(true)
                .message("PIN réinitialisé avec succès")
                .build();
    }


    private String generateResetToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber.length() <= 4) return "****";
        return phoneNumber.substring(0, 4) + "****" + phoneNumber.substring(phoneNumber.length() - 4);
    }

    @lombok.Data
    @lombok.Builder
    private static class ResetTokenData {
        private Long userId;
        private String phoneNumber;
        private boolean otpVerified;
        private boolean securityQuestionsVerified;
        private LocalDateTime expiresAt;

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
    }
}
