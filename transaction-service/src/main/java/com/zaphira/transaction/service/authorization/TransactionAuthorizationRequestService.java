package com.zaphira.transaction.service.authorization;

import com.zaphira.transaction.config.LimitProperties;
import com.zaphira.transaction.dto.AuthorizationRequest;
import com.zaphira.transaction.model.entities.TransactionAuthorization;
import com.zaphira.transaction.model.entities.Transaction;
import com.zaphira.transaction.model.enums.AuthorizationMethod;
import com.zaphira.transaction.model.enums.AuthorizationStatus;
import com.zaphira.transaction.repository.AuthorizationRequestRepository;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class TransactionAuthorizationRequestService {

    private final AuthorizationRequestRepository repository;
    private final LimitProperties limitProperties;

    public TransactionAuthorizationRequestService(AuthorizationRequestRepository repository,
                                           LimitProperties limitProperties) {
        this.repository = repository;
        this.limitProperties = limitProperties;
    }

    public AuthorizationRequest createAuthorization(Transaction transaction,
                                                    AuthorizationMethod method,
                                                    String requestedBy) {
        TransactionAuthorization entity = new TransactionAuthorization();
        entity.setTransaction(transaction);
        entity.setMethod(method);
        entity.setRequestedBy(requestedBy);
        entity.setStatus(AuthorizationStatus.PENDING);
        entity.setChallengeCode(generateChallengeCode(method));
        entity.setExpiresAt(LocalDateTime.now().plus(limitProperties.getAuthorization().getExpiry()));
        TransactionAuthorization saved = repository.save(entity);
        return toDto(saved);
    }

    public AuthorizationRequest approveAuthorization(Long transactionId,
                                                     AuthorizationMethod method,
                                                     String providedCode,
                                                     String actor) {
        TransactionAuthorization entity = repository
                .findTopByTransactionIdAndStatusOrderByRequestedAtDesc(transactionId, AuthorizationStatus.PENDING)
                .orElseThrow(() -> new AuthorizationException("No pending authorization request for transaction"));

        if (!entity.getMethod().equals(method)) {
            throw new AuthorizationException("Authorization method mismatch");
        }
        if (entity.getExpiresAt() != null && entity.getExpiresAt().isBefore(LocalDateTime.now())) {
            entity.setStatus(AuthorizationStatus.EXPIRED);
            repository.save(entity);
            throw new AuthorizationException("Authorization request expired");
        }
        if (!entity.getChallengeCode().equals(providedCode)) {
            throw new AuthorizationException("Invalid authorization code");
        }
        entity.setStatus(AuthorizationStatus.APPROVED);
        entity.setApprovedAt(LocalDateTime.now());
        entity.setApprovedBy(actor);
        TransactionAuthorization saved = repository.save(entity);
        return toDto(saved);
    }

    public AuthorizationRequest getLatestAuthorization(Long transactionId) {
        return repository.findTopByTransactionIdOrderByRequestedAtDesc(transactionId)
                .map(this::toDto)
                .orElse(null);
    }
    private AuthorizationRequest toDto(TransactionAuthorization entity) {
        if (entity == null) return null;
        return AuthorizationRequest.builder()
                .id(entity.getId())
                .transaction(entity.getTransaction())
                .method(entity.getMethod())
                .status(entity.getStatus())
                .requestedBy(entity.getRequestedBy())
                .approvedBy(entity.getApprovedBy())
                .requestedAt(entity.getRequestedAt())
                .approvedAt(entity.getApprovedAt())
                .expiresAt(entity.getExpiresAt())
                .challengeCode(entity.getChallengeCode())
                .rejectionReason(entity.getRejectionReason())
                .build();
    }

    private String generateChallengeCode(AuthorizationMethod method) {
        if (method == AuthorizationMethod.OTP) {
            return RandomStringUtils.secure().nextNumeric(6);
        }
        if (method == AuthorizationMethod.PIN) {
            return RandomStringUtils.secure().nextNumeric(4);
        }
        return RandomStringUtils.secure().nextAlphanumeric(8).toUpperCase();
    }
}


