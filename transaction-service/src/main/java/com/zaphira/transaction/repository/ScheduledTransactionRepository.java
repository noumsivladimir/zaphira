package com.zaphira.transaction.repository;

import com.zaphira.transaction.model.ScheduledTransaction;
import com.zaphira.transaction.model.enums.ScheduledTransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduledTransactionRepository extends JpaRepository<ScheduledTransaction, Long> {

    @Query("select s from ScheduledTransaction s " +
           "where s.status = com.zaphira.transaction.model.enums.ScheduledTransactionStatus.PENDING " +
           "and s.scheduledFor <= :now")
    List<ScheduledTransaction> findDue(LocalDateTime now);

    long countByStatus(ScheduledTransactionStatus status);
}


