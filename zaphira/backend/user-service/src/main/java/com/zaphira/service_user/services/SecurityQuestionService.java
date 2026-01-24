package com.zaphira.service_user.services;

import com.zaphira.service_user.dto.request.SecurityAnswerRequest;
import com.zaphira.service_user.dto.request.SetupSecurityQuestionsRequest;
import com.zaphira.service_user.dto.request.VerifySecurityQuestionsRequest;
import com.zaphira.service_user.dto.response.SecurityQuestionResponse;
import com.zaphira.service_user.dto.response.SecurityQuestionsListResponse;
import com.zaphira.service_user.dto.response.SetupSecurityQuestionsResponse;
import com.zaphira.service_user.model.entities.UserSecurityAnswer;

import java.util.List;

public interface SecurityQuestionService {
    SecurityQuestionsListResponse getAvailableQuestions();
    List<SecurityQuestionResponse> getActiveQuestions();
    SetupSecurityQuestionsResponse setupSecurityQuestions(Long userId, SetupSecurityQuestionsRequest request);
    void saveSecurityAnswersForUser(Long userId, List<SecurityAnswerRequest> answers);
    List<UserSecurityAnswer> getUserSecurityAnswers(Long userId);
    boolean verifyAnswers(Integer userId, List<VerifySecurityQuestionsRequest.SecurityAnswerDto> answers);
    boolean hasConfiguredQuestions(Long userId);
}