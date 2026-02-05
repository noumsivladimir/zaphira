package com.zaphira.user.service;

import com.zaphira.user.model.enums.OtpPurpose;

public interface OtpService {
    String generateOtp(String phoneNumber, OtpPurpose purpose);
    String generateAndSendOtp(String phoneNumber, OtpPurpose purpose);
    String generateAndSendOtp(String phoneNumber, String email, OtpPurpose purpose);
    boolean verifyOtp(String phoneNumber, String code, OtpPurpose purpose);
    boolean verifyOtpByEmail(String email, String code, OtpPurpose purpose);
    void markOtpAsConsumed(String phoneNumber, OtpPurpose purpose);
}

