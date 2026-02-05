package com.zaphira.user.service;

import com.zaphira.user.dto.request.InitiatePinResetRequest;
import com.zaphira.user.dto.request.ResetPinRequest;
import com.zaphira.user.dto.request.VerifyOtpRequest;
import com.zaphira.user.dto.request.VerifySecurityQuestionsRequest;
import com.zaphira.user.dto.response.InitiatePinResetResponse;
import com.zaphira.user.dto.response.ResetPinResponse;
import com.zaphira.user.dto.response.VerifyOtpResponse;
import com.zaphira.user.dto.response.VerifySecurityQuestionsResponse;

public interface PinResetService {
    InitiatePinResetResponse initiateReset(InitiatePinResetRequest request);
    VerifyOtpResponse verifyOtp(VerifyOtpRequest request);
    VerifySecurityQuestionsResponse verifySecurityQuestions(VerifySecurityQuestionsRequest request);
    ResetPinResponse resetPin(ResetPinRequest request);
}
