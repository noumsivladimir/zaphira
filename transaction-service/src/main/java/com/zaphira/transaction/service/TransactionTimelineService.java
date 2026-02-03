package com.zaphira.transaction.service;

import com.zaphira.transaction.model.core.TransactionCore;
import com.zaphira.transaction.model.core.TransactionTimeline;
import com.zaphira.transaction.model.enums.TransactionStatus;
import com.zaphira.transaction.repository.TransactionTimelineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * TransactionTimelineService - LOT 2
 * 
 * Manages transaction state changes and timeline tracking
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionTimelineService {

    private final TransactionTimelineRepository timelineRepository;

    /**
     * Record a status change in timeline
     */
    @Transactional
    public void recordStatusChange(TransactionCore transaction,
                                    TransactionStatus previousStatus,
                                    TransactionStatus newStatus,
                                    String reason) {
        log.debug("Recording status change for transaction {}: {} → {}",
                transaction.getReference(), previousStatus, newStatus);

        TransactionTimeline timeline = TransactionTimeline.builder()
                .transaction(transaction)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .changeReason(reason)
                .changedBy("SYSTEM")
                .build();

        timelineRepository.save(timeline);
    }

    /**
     * Record a status change with details
     */
    @Transactional
    public void recordStatusChange(TransactionCore transaction,
                                    TransactionStatus previousStatus,
                                    TransactionStatus newStatus,
                                    String reason,
                                    String changedBy,
                                    String details) {
        log.debug("Recording detailed status change for transaction {}: {} → {}",
                transaction.getReference(), previousStatus, newStatus);

        TransactionTimeline timeline = TransactionTimeline.builder()
                .transaction(transaction)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .changeReason(reason)
                .changedBy(changedBy)
                .details(details)
                .build();

        timelineRepository.save(timeline);
    }

    /**
     * Get transaction timeline
     */
    @Transactional(readOnly = true)
    public List<TransactionTimeline> getTransactionTimeline(Long transactionId) {
        return timelineRepository.findByTransactionIdOrderByCreatedAtAsc(transactionId);
    }

    /**
     * Count status transitions
     */
    @Transactional(readOnly = true)
    public long countStatusTransitions(TransactionStatus from, TransactionStatus to) {
        return timelineRepository.countStatusTransitions(from, to);
    }
}
