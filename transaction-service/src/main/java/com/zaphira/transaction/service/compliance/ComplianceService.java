//package com.zaphira.transaction.service.compliance;
//
//import com.zaphira.transaction.config.ComplianceProperties;
//import com.zaphira.transaction.dto.requests.TransactionRequest;
//import com.zaphira.transaction.model.entities.Transaction;
//import com.zaphira.transaction.model.enums.ComplianceStatus;
//import org.springframework.stereotype.Component;
//
//import java.math.BigDecimal;
//
//@Component
//public class ComplianceService {
//
//    private final ComplianceProperties properties;
//
//    public ComplianceService(ComplianceProperties properties) {
//        this.properties = properties;
//    }
//
//    /**
//     * Évalue la transaction lors de sa création et met à jour riskScore / complianceStatus.
//     * Règles simples pour démarrer :
//     *  - montant >= highAmountThreshold -> UNDER_REVIEW
//     */
//    public void evaluateOnCreation(TransactionRequest request, Transaction tx) {
//        BigDecimal amount = request.getAmount();
//        BigDecimal threshold = properties.getHighAmountThreshold();
//
//        int score = 0;
//
//        if (amount != null && threshold != null && amount.compareTo(threshold) >= 0) {
//            score += 80;
//        }
//
//        tx.setRiskScore(score);
//
//        if (score >= 80) {
//            tx.setComplianceStatus(ComplianceStatus.UNDER_REVIEW);
//        } else {
//            tx.setComplianceStatus(ComplianceStatus.CLEAR);
//        }
//    }
//
//    /**
//     * Vérifie, avant exécution, que la transaction n'est pas bloquée par la compliance.
//     */
//    public void assertNotBlocked(Transaction tx) {
//        if (tx.getComplianceStatus() == ComplianceStatus.BLOCKED) {
//            throw new ComplianceException("Transaction is blocked by compliance rules");
//        }
//    }
//}
//
//
