package com.zaphira.service_user.controller;

import com.zaphira.service_user.dto.request.ChangePinRequest;
import com.zaphira.service_user.dto.request.UpdateProfileRequest;
import com.zaphira.service_user.dto.request.VerifyEmailRequest;
import com.zaphira.service_user.dto.request.VerifyOtpRequest;
import com.zaphira.service_user.dto.response.UserNotificationInfoResponse;
import com.zaphira.service_user.dto.response.ApiResponse;
import com.zaphira.service_user.dto.response.UsersRegistrationResponse;
import com.zaphira.service_user.dto.request.UserRegistrationRequest;
import com.zaphira.service_user.dto.response.UserResponse;
import com.zaphira.service_user.dto.response.ChangePinResponse;
import com.zaphira.service_user.dto.response.UserVerificationStatusResponse;
import com.zaphira.service_user.dto.response.UserSecurityQuestionResponse;
import com.zaphira.service_user.model.entities.User;
import com.zaphira.service_user.services.UserRegistrationServiceImpl;
import com.zaphira.service_user.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "User profile and account management endpoints")

public class UserController {

    private final UserService userService;
    private final UserRegistrationServiceImpl userRegistrationService;
//    private final AuthService authService;



    @PostMapping("/register")
    @Operation(summary = "Register new user", description = "Register a new regular user account with wallet creation")
    public ResponseEntity<ApiResponse<UsersRegistrationResponse>> registerUser(
            @Valid @RequestBody UserRegistrationRequest request) {

        log.info("Registration request received for phone: {}", request.getPhoneNumber());
        UsersRegistrationResponse user = userRegistrationService.registerRegularUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(user, "User registered successfully. Please check your email for verification code."));
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify email and activate account", description = "Verify email with OTP code and activate user account")
    public ResponseEntity<ApiResponse<UsersRegistrationResponse>> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request) {

        log.info("Email verification request received for: {}", request.getEmail());
        UsersRegistrationResponse user = userRegistrationService.verifyEmailAndActivateAccount(
                request.getEmail(), request.getVerificationCode());
        return ResponseEntity.ok(ApiResponse.success(user, "Email verified successfully. Account activated."));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify OTP and activate account", description = "Verify OTP code for registration and activate user account")
    public ResponseEntity<ApiResponse<UsersRegistrationResponse>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {

        log.info("OTP verification request received for phone: {}", request.getPhoneNumber());
        UsersRegistrationResponse user = userRegistrationService.verifyOtpAndActivateAccount(
                request.getPhoneNumber(), request.getOtpCode());
        return ResponseEntity.ok(ApiResponse.success(user, "OTP verified successfully. Account activated."));
    }

    @PostMapping("/send-otp/{userId}")
    @Operation(summary = "Send OTP for user verification", description = "Send OTP code to user's phone number for account verification")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendOtpForUser(@PathVariable Long userId) {

        log.info("Send OTP request received for user: {}", userId);
        Map<String, Object> response = userRegistrationService.sendOtpForUser(userId);
        return ResponseEntity.ok(ApiResponse.success(response, "OTP sent successfully"));
    }

    @PostMapping("/generate-email-otp/{userId}")
    @Operation(summary = "Generate email OTP for user verification", description = "Generate and send OTP code to user's email address for account verification")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateEmailOtpForUser(@PathVariable Long userId) {

        log.info("Generate email OTP request received for user: {}", userId);
        Map<String, Object> response = userRegistrationService.generateOtpForUser(userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Email OTP sent successfully"));
    }

    @GetMapping("/verify-email-link")
    @Operation(summary = "Verify email via link", description = "Verify email by clicking on the link sent in the email")
    public ResponseEntity<String> verifyEmailViaLink(
            @RequestParam String email,
            @RequestParam String code) {

        log.info("Email verification link clicked for: {}", email);

        try {
            UsersRegistrationResponse user = userRegistrationService.verifyEmailAndActivateAccount(email, code);

            // Retourner une page HTML de succès
            String htmlResponse = String.format(
                "<!DOCTYPE html>" +
                "<html lang='fr'>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<title>Email Vérifié - Zaphira</title>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; text-align: center; padding: 50px; background-color: #f5f5f5; }" +
                ".container { max-width: 600px; margin: 0 auto; background: white; padding: 40px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }" +
                ".success { color: #28a745; font-size: 48px; margin-bottom: 20px; }" +
                ".message { font-size: 18px; color: #333; margin-bottom: 30px; }" +
                ".user-info { background: #f8f9fa; padding: 20px; border-radius: 5px; margin: 20px 0; }" +
                ".button { display: inline-block; padding: 12px 24px; background: #007bff; color: white; text-decoration: none; border-radius: 5px; margin-top: 20px; }" +
                ".button:hover { background: #0056b3; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='success'>✓</div>" +
                "<h1>Email Vérifié avec Succès !</h1>" +
                "<p class='message'>Votre compte Zaphira a été activé. Vous pouvez maintenant vous connecter et commencer à utiliser nos services.</p>" +
                "<div class='user-info'>" +
                "<strong>Informations du compte :</strong><br>" +
                "Email : %s<br>" +
                "Nom : %s %s<br>" +
                "Statut : %s" +
                "</div>" +
                "<a href='http://localhost:3000/login' class='button'>Se Connecter</a>" +
                "</div>" +
                "</body>" +
                "</html>",
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getAccountStatus()
            );

            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.TEXT_HTML)
                    .body(htmlResponse);

        } catch (Exception e) {
            log.error("Email verification failed for: {}", email, e);

            // Retourner une page HTML d'erreur
            String errorHtml = String.format(
                "<!DOCTYPE html>" +
                "<html lang='fr'>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<title>Erreur de Vérification - Zaphira</title>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; text-align: center; padding: 50px; background-color: #f5f5f5; }" +
                ".container { max-width: 600px; margin: 0 auto; background: white; padding: 40px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }" +
                ".error { color: #dc3545; font-size: 48px; margin-bottom: 20px; }" +
                ".message { font-size: 18px; color: #333; margin-bottom: 30px; }" +
                ".button { display: inline-block; padding: 12px 24px; background: #dc3545; color: white; text-decoration: none; border-radius: 5px; margin-top: 20px; }" +
                ".button:hover { background: #c82333; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='error'>✗</div>" +
                "<h1>Erreur de Vérification</h1>" +
                "<p class='message'>Le lien de vérification est invalide ou a expiré. Veuillez réessayer ou contacter le support.</p>" +
                "<a href='http://localhost:3000/register' class='button'>Retour à l'Inscription</a>" +
                "</div>" +
                "</body>" +
                "</html>"
            );

            return ResponseEntity.badRequest()
                    .contentType(org.springframework.http.MediaType.TEXT_HTML)
                    .body(errorHtml);
        }
    }

    @GetMapping("/profile")
    @Operation(summary = "Get current user profile", description = "Get authenticated user's profile")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(
            @RequestAttribute("userId") Long userId) {

        UserResponse user = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PutMapping("/profile")
    @Operation(summary = "Update profile", description = "Update user profile information")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody UpdateProfileRequest request) {

        UserResponse user = userService.updateUserProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success(user, "Profile updated successfully"));
    }

    @PutMapping("/{walletId}/pin")
    @Operation(summary = "Update PIN", description = "Update user PIN")
    public ResponseEntity<ApiResponse<ChangePinResponse>> updatePin(@PathVariable  Integer walletId,
                                                               @Valid @RequestBody ChangePinRequest request) {
        ChangePinResponse user = userService.changePin(walletId.toString(), request);
        return ResponseEntity.ok(ApiResponse.success(user, "PIN updated successfully"));
    }


    @PostMapping("/profile/picture")
    @Operation(summary = "Upload profile picture", description = "Upload or update profile picture")
    public ResponseEntity<ApiResponse<String>> uploadProfilePicture(
            @RequestAttribute("userId") Long userId,
            @RequestParam("file") MultipartFile file) {

        String fileUrl = userService.uploadProfilePicture(userId, file);
        return ResponseEntity.ok(ApiResponse.success(fileUrl, "Profile picture uploaded successfully"));
    }

    @DeleteMapping("/profile")
    @Operation(summary = "Delete account with Wallet Id", description = "Soft delete user account")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @RequestAttribute("walletId") Integer walletId) {


        User user = userService.findUserEntityByWalletId(walletId.toString());
        userService.softDeleteUser(user.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "Account deleted successfully"));
    }

    @GetMapping("/{userIdOrWalletId}")
    @Operation(summary = "Get user by ID or Wallet Id", description = "Get user details by user ID")
    public ResponseEntity<ApiResponse<UserResponse>> getUserByIdOrWalletId(
            @PathVariable Integer userIdOrWalletId) {

        if (userIdOrWalletId  >= 10_000_000 && userIdOrWalletId <= 99_999_999) {

            UserResponse user = userService.getUserByWalletId(userIdOrWalletId.toString());
            return ResponseEntity.ok(ApiResponse.success(user));

        }

        UserResponse user = userService.getUserById(userIdOrWalletId.longValue());
        return ResponseEntity.ok(ApiResponse.success(user));
    }


    @GetMapping("/question/{walletId}")
    @Operation(summary = "list de question", description = "")
    public ResponseEntity<ApiResponse<List<UserSecurityQuestionResponse>>> findSecurityQuestionByWalletId(
            @PathVariable("walletId") String walletId) {



        List<UserSecurityQuestionResponse> userSecurityAnswers = userService.findSecurityQuestionByWalletId(walletId);
//        List<UserSecurityAnswer> findSecurityQuestionByUserId(Long userId)

        return ResponseEntity.ok(ApiResponse.success(userSecurityAnswers));


    }

    @GetMapping("/{userId}/notification-info")
    @Operation(summary = "Get user notification info", description = "Get user information needed for notifications")
    public ResponseEntity<UserNotificationInfoResponse> getUserNotificationInfo(@PathVariable Long userId) {
        UserNotificationInfoResponse info = userService.getUserNotificationInfo(userId);
        return ResponseEntity.ok(info);
    }
//
//        String jwtToken = token.replace("Bearer ", "");
//        List<SessionResponse> sessions = authService.getActiveSessions(userId, jwtToken);
//        return ResponseEntity.ok(ApiResponse.success(sessions));
//    }
//
//    @DeleteMapping("/sessions/{sessionId}")
//    @Operation(summary = "Terminate session", description = "Logout from specific device/session")
//    public ResponseEntity<ApiResponse<Void>> terminateSession(
//            @RequestAttribute("userId") Long userId,
//            @PathVariable Integer sessionId) {
//dd
//        authService.terminateSession(userId, sessionId);
//        return ResponseEntity.ok(ApiResponse.success(null, "Session terminated successfully"));
//    }
//
//    @DeleteMapping("/sessions/all")
//    @Operation(summary = "Logout all devices", description = "Logout from all devices")
//    public ResponseEntity<ApiResponse<Void>> logoutAllDevices(
//            @RequestAttribute("userId") Long userId) {
//
//        authService.logoutAllDevices(userId);
//        return ResponseEntity.ok(ApiResponse.success(null, "Logged out from all devices"));
//    }
//
//    @PostMapping("/2fa/enable")
//    @Operation(summary = "Enable 2FA", description = "Enable two-factor authentication")
//    public ResponseEntity<ApiResponse<TwoFactorSetupResponse>> enableTwoFactor(
//            @RequestAttribute("userId") Long userId) {
//
//        TwoFactorSetupResponse response = authService.enableTwoFactorAuth(userId);
//        return ResponseEntity.ok(ApiResponse.success(response, "2FA enabled successfully"));
//    }
//
//    @PostMapping("/2fa/disable")
//    @Operation(summary = "Disable 2FA", description = "Disable two-factor authentication")
//    public ResponseEntity<ApiResponse<Void>> disableTwoFactor(
//            @RequestAttribute("userId") Long userId,
//            @RequestParam String password) {
//
//        authService.disableTwoFactorAuth(userId, password);
//        return ResponseEntity.ok(ApiResponse.success(null, "2FA disabled successfully"));
//    }

//    @PostMapping("/2fa/verify")
//    @Operation(summary = "Verify 2FA code", description = "Verify two-factor authentication code")
//    public ResponseEntity<ApiResponse<Boolean>> verifyTwoFactorCode(
//            @RequestAttribute("userId") Long userId,
//            @RequestParam String code) {
//
//        boolean isValid = authService.verifyTwoFactorCode(userId, code);
//
//        if (isValid) {
//            return ResponseEntity.ok(ApiResponse.success(true, "2FA code verified"));
//        } else {
//            return ResponseEntity.badRequest()
//                    .body(ApiResponse.error("Invalid 2FA code"));
//        }
//    }

    @GetMapping("/verification-status")
    @Operation(summary = "Check user verification status", description = "Check if a user email is already verified")
    public ResponseEntity<ApiResponse<UserVerificationStatusResponse>> getUserVerificationStatus(
            @RequestParam("email") String email) {

        log.info("Checking verification status for email: {}", email);

        try {
            User user = userService.findUserEntityByEmail(email);

            if (user == null) {
                return ResponseEntity.ok(ApiResponse.success(
                    new UserVerificationStatusResponse(false, "USER_NOT_FOUND", email),
                    "User not found"));
            }

            boolean isVerified = "ACTIVE".equals(user.getAccountStatus().name());
            String status = isVerified ? "VERIFIED" : "PENDING_VERIFICATION";

            UserVerificationStatusResponse response = new UserVerificationStatusResponse(isVerified, status, email);

            return ResponseEntity.ok(ApiResponse.success(response, "Verification status retrieved successfully"));

        } catch (Exception e) {
            log.error("Error checking verification status for email: {}", email, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to check verification status"));
        }
    }
}