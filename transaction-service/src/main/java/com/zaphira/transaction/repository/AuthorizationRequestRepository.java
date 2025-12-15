package com.zaphira.transaction.repository;

import com.zaphira.transaction.model.AuthorizationRequest;
import com.zaphira.transaction.model.enums.AuthorizationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthorizationRequestRepository extends JpaRepository<AuthorizationRequest, Long> {

    Optional<AuthorizationRequest> findTopByTransactionIdOrderByRequestedAtDesc(Long transactionId);

    Optional<AuthorizationRequest> findTopByTransactionIdAndStatusOrderByRequestedAtDesc(Long transactionId,
                                                                                         AuthorizationStatus status);
}


