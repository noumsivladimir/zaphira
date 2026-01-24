package com.zaphira.service_user.services;

import com.zaphira.service_user.dto.request.SecurityAnswerRequest;
import com.zaphira.service_user.dto.request.SetupSecurityQuestionsRequest;
import com.zaphira.service_user.dto.request.VerifySecurityQuestionsRequest;
import com.zaphira.service_user.dto.response.SecurityQuestionResponse;
import com.zaphira.service_user.dto.response.SecurityQuestionsListResponse;
import com.zaphira.service_user.dto.response.SetupSecurityQuestionsResponse;
import com.zaphira.service_user.model.entities.PredefinedSecurityQuestion;
import com.zaphira.service_user.model.entities.User;
import com.zaphira.service_user.model.entities.UserSecurityAnswer;
import com.zaphira.service_user.repository.PredefinedSecurityQuestionRepository;
import com.zaphira.service_user.repository.UserRepository;
import com.zaphira.service_user.repository.UserSecurityAnswerRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

            @Override
            public List<SecurityQuestionResponse> getActiveQuestions() {
            return predefinedQuestionRepository.findByActiveTrueOrderByDisplayOrder()
                .stream()
                .map(q -> SecurityQuestionResponse.builder()
                    .id(q.getId())
                    .question(q.getQuestion())
                    .build())
                .toList();
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
                    .findByIdAndActiveTrue(answerSetup.getQuestionId())
                    .orElseThrow(() -> new IllegalArgumentException("Question inactive ou inexistante: " + answerSetup.getQuestionId()));

                String salt = generateSalt();
                String normalizedAnswer = normalizeAnswer(answerSetup.getAnswer());
                String hashedAnswer = pinService.hashPin(buildSaltedAnswer(normalizedAnswer, salt));

            UserSecurityAnswer answer = UserSecurityAnswer.builder()
                    .user(user)
                    .question(question)
                    .answerHash(hashedAnswer)
                    .answerSalt(salt)
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
                String saltedAnswer = buildSaltedAnswer(normalizedAnswer, userAnswer.getAnswerSalt());
                if (pinService.verifyPin(saltedAnswer, userAnswer.getAnswerHash())) {
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

    @Override
    @Transactional
    public void saveSecurityAnswersForUser(Long userId, List<SecurityAnswerRequest> answers) {
        if (answers == null || answers.isEmpty()) {
            throw new IllegalArgumentException("Les réponses aux questions de sécurité sont requises");
        }

        if (answers.size() < MIN_QUESTIONS || answers.size() > MAX_QUESTIONS) {
            throw new IllegalArgumentException("Vous devez configurer entre " + MIN_QUESTIONS + " et " + MAX_QUESTIONS + " questions");
        }

        Set<Long> uniqueIds = answers.stream()
                .map(SecurityAnswerRequest::getQuestionId)
                .collect(Collectors.toSet());
        if (uniqueIds.size() != answers.size()) {
            throw new IllegalArgumentException("Vous ne pouvez pas choisir la même question plusieurs fois");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));

        for (SecurityAnswerRequest answer : answers) {
            PredefinedSecurityQuestion question = predefinedQuestionRepository
                    .findByIdAndActiveTrue(answer.getQuestionId())
                    .orElseThrow(() -> new IllegalArgumentException("Question inactive ou inexistante: " + answer.getQuestionId()));

            if (userAnswerRepository.existsByUser_IdAndQuestion_Id(userId, question.getId())) {
                throw new IllegalArgumentException("La question est déjà configurée pour cet utilisateur");
            }

            String salt = generateSalt();
            String normalizedAnswer = normalizeAnswer(answer.getAnswer());
            String hashedAnswer = pinService.hashPin(buildSaltedAnswer(normalizedAnswer, salt));

            UserSecurityAnswer entity = UserSecurityAnswer.builder()
                    .user(user)
                    .question(question)
                    .answerHash(hashedAnswer)
                    .answerSalt(salt)
                    .createdAt(LocalDateTime.now())
                    .build();

            userAnswerRepository.save(entity);
        }
    }

    // Normaliser la réponse (minuscule, sans espaces superflus)
    private String normalizeAnswer(String answer) {
        return answer.toLowerCase().trim().replaceAll("\\s+", " ");
    }

    private String generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    private String buildSaltedAnswer(String normalizedAnswer, String salt) {
        return normalizedAnswer + ":" + salt;
    }
}