package com.zaphira.service_user.services;

import com.zaphira.service_user.dto.request.InitiatePinResetRequest;
import com.zaphira.service_user.dto.request.ResetPinRequest;
import com.zaphira.service_user.dto.request.VerifyOtpRequest;
import com.zaphira.service_user.dto.request.VerifySecurityQuestionsRequest;
import com.zaphira.service_user.dto.response.InitiatePinResetResponse;
import com.zaphira.service_user.dto.response.ResetPinResponse;
import com.zaphira.service_user.dto.response.VerifyOtpResponse;
import com.zaphira.service_user.dto.response.VerifySecurityQuestionsResponse;

public interface PinResetService {
    InitiatePinResetResponse initiateReset(InitiatePinResetRequest request);
    VerifyOtpResponse verifyOtp(VerifyOtpRequest request);
    VerifySecurityQuestionsResponse verifySecurityQuestions(VerifySecurityQuestionsRequest request);
    ResetPinResponse resetPin(ResetPinRequest request);
}