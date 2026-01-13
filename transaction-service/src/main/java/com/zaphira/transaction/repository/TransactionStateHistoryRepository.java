package com.zaphira.transaction.repository;

import com.zaphira.transaction.model.entities.Transaction;
import com.zaphira.transaction.model.entities.TransactionStateHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionStateHistoryRepository extends JpaRepository<TransactionStateHistory, Long> {

    List<TransactionStateHistory> findByTransactionOrderByChangedAtAsc(Transaction transaction);
}


