package com.zaphira.service_user.services;

import com.zaphira.service_user.dto.request.SetupSecurityQuestionsRequest;
import com.zaphira.service_user.dto.request.VerifySecurityQuestionsRequest;
import com.zaphira.service_user.dto.response.SecurityQuestionsListResponse;
import com.zaphira.service_user.dto.response.SetupSecurityQuestionsResponse;
import com.zaphira.service_user.model.entities.UserSecurityAnswer;

import java.util.List;

public interface SecurityQuestionService {
    SecurityQuestionsListResponse getAvailableQuestions();
    SetupSecurityQuestionsResponse setupSecurityQuestions(Long userId, SetupSecurityQuestionsRequest request);
    List<UserSecurityAnswer> getUserSecurityAnswers(Long userId);
    boolean verifyAnswers(Integer userId, List<VerifySecurityQuestionsRequest.SecurityAnswerDto> answers);
    boolean hasConfiguredQuestions(Long userId);
}