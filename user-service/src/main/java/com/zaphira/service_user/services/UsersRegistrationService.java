package com.zaphira.service_user.services;

import com.zaphira.service_user.dto.request.UserRegistrationRequest;
import com.zaphira.service_user.dto.response.UsersRegistrationResponse;

public interface UsersRegistrationService   {

    // User Registration
    UsersRegistrationResponse registerRegularUser(UserRegistrationRequest request);
    UsersRegistrationResponse registerAdmin(UserRegistrationRequest request);
    UsersRegistrationResponse registerMerchant(UserRegistrationRequest request);
    UsersRegistrationResponse registerSupport(UserRegistrationRequest request);
    
    // Email Verification
    UsersRegistrationResponse verifyEmailAndActivateAccount(String email, String verificationCode);
}
