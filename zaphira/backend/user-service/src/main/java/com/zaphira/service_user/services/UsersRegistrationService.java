package com.zaphira.service_user.services;

import com.zaphira.service_user.dto.request.UserRegistrationRequest;
import com.zaphira.service_user.dto.response.UsersRegistrationResponse;

import java.util.Map;

public interface UsersRegistrationService   {

    // User Registration
    UsersRegistrationResponse registerRegularUser(UserRegistrationRequest request);
    UsersRegistrationResponse registerAdmin(UserRegistrationRequest request);
    UsersRegistrationResponse registerMerchant(UserRegistrationRequest request);
    UsersRegistrationResponse registerSupport(UserRegistrationRequest request);
    
    // Email Verification
    UsersRegistrationResponse verifyEmailAndActivateAccount(String email, String verificationCode);

    // OTP
    Map<String, Object> sendOtpForUser(Long userId);
    Map<String, Object> generateOtpForUser(Long userId); // Nouvelle méthode
    UsersRegistrationResponse verifyOtpAndActivateAccount(String phoneNumber, String otpCode);
}
