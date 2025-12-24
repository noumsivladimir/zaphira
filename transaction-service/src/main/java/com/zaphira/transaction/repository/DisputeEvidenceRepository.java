package com.zaphira.transaction.repository;

import com.zaphira.transaction.model.Dispute;
import com.zaphira.transaction.model.DisputeEvidence;
import com.zaphira.transaction.model.enums.EvidenceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * DisputeEvidenceRepository - Data access layer for DisputeEvidence entities.
 * 
 * Provides custom query methods for evidence management:
 * - Find by dispute
 * - Find by evidence type
 * - Find unverified evidence (for admin review)
 * 
 * Used by DisputeService for evidence submission and retrieval.
 */
@Repository
public interface DisputeEvidenceRepository extends JpaRepository<DisputeEvidence, Long> {
    
    /**
     * Find all evidence for a specific dispute.
     * Used when retrieving dispute details.
     */
    List<DisputeEvidence> findByDispute(Dispute dispute);
    
    /**
     * Find evidence of specific type for a dispute.
     */
    List<DisputeEvidence> findByDisputeAndEvidenceType(Dispute dispute, EvidenceType evidenceType);
    
    /**
     * Find all unverified evidence.
     * Used by admin team to review and verify evidence.
     */
    @Query("SELECT de FROM DisputeEvidence de WHERE de.verified = false ORDER BY de.submittedAt ASC")
    List<DisputeEvidence> findUnverifiedEvidence();
    
    /**
     * Count evidence items for a dispute.
     * Used in dispute details response.
     */
    long countByDispute(Dispute dispute);
}
