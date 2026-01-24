package com.zaphira.transaction.service.authorization;

import com.zaphira.transaction.config.LimitProperties;
import com.zaphira.transaction.model.AuthorizationRequest;
import com.zaphira.transaction.model.Transaction;
import com.zaphira.transaction.model.enums.AuthorizationMethod;
import com.zaphira.transaction.model.enums.AuthorizationStatus;
import com.zaphira.transaction.repository.AuthorizationRequestRepository;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthorizationRequestService {

    private final AuthorizationRequestRepository repository;
    private final LimitProperties limitProperties;

    public AuthorizationRequestService(AuthorizationRequestRepository repository,
                                           LimitProperties limitProperties) {
        this.repository = repository;
        this.limitProperties = limitProperties;
    }

    public AuthorizationRequest createAuthorization(Transaction transaction,
                                                    AuthorizationMethod method,
                                                    String requestedBy) {
        AuthorizationRequest request = AuthorizationRequest.builder()
                .transaction(transaction)
                .method(method)
                .requestedBy(requestedBy)
                .status(AuthorizationStatus.PENDING)
                .challengeCode(generateChallengeCode(method))
                .expiresAt(LocalDateTime.now().plus(limitProperties.getAuthorization().getExpiry()))
                .build();
        return repository.save(request);
    }

    public AuthorizationRequest approveAuthorization(Long transactionId,
                                                     AuthorizationMethod method,
                                                     String providedCode,
                                                     String actor) {
        AuthorizationRequest request = repository
                .findTopByTransactionIdAndStatusOrderByRequestedAtDesc(transactionId, AuthorizationStatus.PENDING)
                .orElseThrow(() -> new AuthorizationException("No pending authorization request for transaction"));

        if (!request.getMethod().equals(method)) {
            throw new AuthorizationException("Authorization method mismatch");
        }
        if (request.isExpired()) {
            request.setStatus(AuthorizationStatus.EXPIRED);
            repository.save(request);
            throw new AuthorizationException("Authorization request expired");
        }
        if (!request.getChallengeCode().equals(providedCode)) {
            throw new AuthorizationException("Invalid authorization code");
        }
        request.setStatus(AuthorizationStatus.APPROVED);
        request.setApprovedAt(LocalDateTime.now());
        request.setApprovedBy(actor);
        return repository.save(request);
    }

    public AuthorizationRequest getLatestAuthorization(Long transactionId) {
        return repository.findTopByTransactionIdOrderByRequestedAtDesc(transactionId)
                .orElse(null);
    }

    private String generateChallengeCode(AuthorizationMethod method) {
        if (method == AuthorizationMethod.OTP) {
            return RandomStringUtils.randomNumeric(6);
        }
        if (method == AuthorizationMethod.PIN) {
            return RandomStringUtils.randomNumeric(4);
        }
        return RandomStringUtils.randomAlphanumeric(8).toUpperCase();
    }
}


