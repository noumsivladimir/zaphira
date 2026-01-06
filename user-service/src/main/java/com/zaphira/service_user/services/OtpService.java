package com.zaphira.service_user.services;

import com.zaphira.service_user.model.enums.OtpPurpose;

public interface OtpService {
    String generateAndSendOtp(String phoneNumber, OtpPurpose purpose);
    String generateAndSendOtp(String phoneNumber, String email, OtpPurpose purpose);
    boolean verifyOtp(String phoneNumber, String code, OtpPurpose purpose);
    boolean verifyOtpByEmail(String email, String code, OtpPurpose purpose);
}

