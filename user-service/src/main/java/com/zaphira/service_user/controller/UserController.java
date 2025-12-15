package com.zaphira.service_user.controller;

import com.zaphira.service_user.dto.request.UpdateProfileRequest;
import com.zaphira.service_user.dto.request.UserRegistrationRequest;
import com.zaphira.service_user.dto.response.ApiResponse;
import com.zaphira.service_user.dto.response.UserResponse;
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


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "User profile and account management endpoints")

public class UserController {

    private final UserService userService;
//    private final AuthService authService;



    @PostMapping("/register")
    @Operation(summary = "Register new user", description = "Register a new regular user account with wallet creation")
    public ResponseEntity<ApiResponse<UserResponse>> registerUser(
            @Valid @RequestBody UserRegistrationRequest request) {

        log.info("Registration request received for phone: {}", request.getPhoneNumber());
        UserResponse user = userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(user, "User registered successfully"));
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

    @PostMapping("/profile/picture")
    @Operation(summary = "Upload profile picture", description = "Upload or update profile picture")
    public ResponseEntity<ApiResponse<String>> uploadProfilePicture(
            @RequestAttribute("userId") Long userId,
            @RequestParam("file") MultipartFile file) {

        String fileUrl = userService.uploadProfilePicture(userId, file);
        return ResponseEntity.ok(ApiResponse.success(fileUrl, "Profile picture uploaded successfully"));
    }

    @DeleteMapping("/profile")
    @Operation(summary = "Delete account", description = "Soft delete user account")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @RequestAttribute("userId") Long userId) {

        userService.softDeleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Account deleted successfully"));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID", description = "Get user details by user ID")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @PathVariable Long userId) {

        UserResponse user = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

//    @GetMapping("/sessions")
//    @Operation(summary = "Get active sessions", description = "Get all active sessions for current user")
//    public ResponseEntity<ApiResponse<List<SessionResponse>>> getActiveSessions(
//            @RequestAttribute("userId") Long userId,
//            @RequestHeader("Authorization") String token) {
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
//            @PathVariable Long sessionId) {
//
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
}