package com.zaphira.transaction.repository;

import com.zaphira.transaction.model.entities.TransactionAuthorization;
import com.zaphira.transaction.model.enums.AuthorizationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthorizationRequestRepository extends JpaRepository<TransactionAuthorization, Long> {

    Optional<TransactionAuthorization> findTopByTransactionIdOrderByRequestedAtDesc(Long transactionId);

    Optional<TransactionAuthorization> findTopByTransactionIdAndStatusOrderByRequestedAtDesc(Long transactionId,
                                                                                         AuthorizationStatus status);
}


