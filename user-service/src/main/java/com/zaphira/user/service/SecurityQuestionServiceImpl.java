package com.zaphira.user.service;

import com.zaphira.user.dto.request.SetupSecurityQuestionsRequest;
import com.zaphira.user.dto.request.VerifySecurityQuestionsRequest;
import com.zaphira.user.dto.response.SecurityQuestionsListResponse;
import com.zaphira.user.dto.response.SetupSecurityQuestionsResponse;
import com.zaphira.user.model.entities.PredefinedSecurityQuestion;
import com.zaphira.user.model.entities.User;
import com.zaphira.user.model.entities.UserSecurityAnswer;
import com.zaphira.user.repository.PredefinedSecurityQuestionRepository;
import com.zaphira.user.repository.UserRepository;
import com.zaphira.user.repository.UserSecurityAnswerRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityQuestionServiceImpl implements SecurityQuestionService {

    private final PredefinedSecurityQuestionRepository predefinedQuestionRepository;
    private final UserSecurityAnswerRepository userAnswerRepository;
    private final UserRepository userRepository;
    private final PinService pinService;

    private static final int MIN_QUESTIONS = 2;
    private static final int MAX_QUESTIONS = 3;

    @Override
    public SecurityQuestionsListResponse getAvailableQuestions() {
        List<PredefinedSecurityQuestion> questions = predefinedQuestionRepository
                .findByActiveTrueOrderByDisplayOrder();

        List<SecurityQuestionsListResponse.QuestionDto> questionDtos = questions.stream()
                .map(q -> SecurityQuestionsListResponse.QuestionDto.builder()
                        .id(q.getId())
                        .question(q.getQuestion())
                        .build())
                .toList();

        return SecurityQuestionsListResponse.builder()
                .questions(questionDtos)
                .build();
    }

    //@Override
    @Transactional
    public SetupSecurityQuestionsResponse setupSecurityQuestions(Long userId, SetupSecurityQuestionsRequest request) {
        log.info("Setting up security questions for user: {}", userId);

        // Vérifier que l'utilisateur existe
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Vérifier le nombre de questions
        if (request.getAnswers().size() < MIN_QUESTIONS || request.getAnswers().size() > MAX_QUESTIONS) {
            return SetupSecurityQuestionsResponse.builder()
                    .success(false)
                    .message("Vous devez configurer entre " + MIN_QUESTIONS + " et " + MAX_QUESTIONS + " questions")
                    .build();
        }

        // Vérifier que toutes les questions sont différentes
        long uniqueQuestions = request.getAnswers().stream()
                .map(SetupSecurityQuestionsRequest.SecurityAnswerSetup::getQuestionId)
                .distinct()
                .count();

        if (uniqueQuestions != request.getAnswers().size()) {
            return SetupSecurityQuestionsResponse.builder()
                    .success(false)
                    .message("Vous ne pouvez pas choisir la même question plusieurs fois")
                    .build();
        }

        // Supprimer les anciennes réponses
        //userAnswerRepository.deleteByUser_Id(userId);

        // Sauvegarder les nouvelles réponses
        for (SetupSecurityQuestionsRequest.SecurityAnswerSetup answerSetup : request.getAnswers()) {
            PredefinedSecurityQuestion question = predefinedQuestionRepository
                    .findById(answerSetup.getQuestionId())
                    .orElseThrow(() -> new RuntimeException("Question non trouvée: " + answerSetup.getQuestionId()));

            UserSecurityAnswer answer = UserSecurityAnswer.builder()
                    .user(user)
                    .question(question)
                    .answerHash(pinService.hashPin(normalizeAnswer(answerSetup.getAnswer())))
                    .createdAt(LocalDateTime.now())
                    .build();

            userAnswerRepository.save(answer);
        }

        log.info("Security questions configured successfully for user: {}", userId);

        return SetupSecurityQuestionsResponse.builder()
                .success(true)
                .message("Questions de sécurité configurées avec succès")
                .questionsConfigured(request.getAnswers().size())
                .build();
    }





    @Override
    public List<UserSecurityAnswer> getUserSecurityAnswers(Long userId) {

        return userAnswerRepository.findByUserQuestionId(userId);
    }

    @Override
    public boolean verifyAnswers(Integer userId, List<VerifySecurityQuestionsRequest.SecurityAnswerDto> answers) {
        return false;
    }

    // @Override
    public boolean verifyAnswers(Long userId, List<VerifySecurityQuestionsRequest.SecurityAnswerDto> answers) {
        List<UserSecurityAnswer> userAnswers = userAnswerRepository.findByUserQuestionId(userId);

        if (userAnswers.isEmpty()) {
            log.warn("No security questions configured for user: {}", userId);
            return false;
        }

        int correctCount = 0;
        for (VerifySecurityQuestionsRequest.SecurityAnswerDto providedAnswer : answers) {
            UserSecurityAnswer userAnswer = userAnswers.stream()
                    .filter(ua -> ua.getQuestion().getId().equals(providedAnswer.getQuestionId()))
                    .findFirst()
                    .orElse(null);

            if (userAnswer != null) {
                String normalizedAnswer = normalizeAnswer(providedAnswer.getAnswer());
                if (pinService.verifyPin(normalizedAnswer, userAnswer.getAnswerHash())) {
                    correctCount++;
                }
            }
        }

        // Toutes les réponses doivent être correctes
        return correctCount == userAnswers.size();
    }

    @Override
    public boolean hasConfiguredQuestions(Long userId) {
        return userAnswerRepository.numberOfAnswers(userId) >= MIN_QUESTIONS;
    }

    // Normaliser la réponse (minuscule, sans espaces superflus)
    private String normalizeAnswer(String answer) {
        return answer.toLowerCase().trim().replaceAll("\\s+", " ");
    }
}
