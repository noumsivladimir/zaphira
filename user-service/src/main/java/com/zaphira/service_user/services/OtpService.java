package com.zaphira.service_user.services;

import com.zaphira.service_user.model.enums.OtpPurpose;

public interface OtpService {
    String generateAndSendOtp(String phoneNumber, OtpPurpose purpose);
    boolean verifyOtp(String phoneNumber, String code, OtpPurpose purpose);
}

