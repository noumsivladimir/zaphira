package com.zaphira.user.service;

import com.zaphira.user.dto.request.SetupSecurityQuestionsRequest;
import com.zaphira.user.dto.request.VerifySecurityQuestionsRequest;
import com.zaphira.user.dto.response.SecurityQuestionsListResponse;
import com.zaphira.user.dto.response.SetupSecurityQuestionsResponse;
import com.zaphira.user.model.entities.UserSecurityAnswer;

import java.util.List;

public interface SecurityQuestionService {
    SecurityQuestionsListResponse getAvailableQuestions();
    SetupSecurityQuestionsResponse setupSecurityQuestions(Long userId, SetupSecurityQuestionsRequest request);
    List<UserSecurityAnswer> getUserSecurityAnswers(Long userId);
    boolean verifyAnswers(Integer userId, List<VerifySecurityQuestionsRequest.SecurityAnswerDto> answers);
    boolean hasConfiguredQuestions(Long userId);
}
