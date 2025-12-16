package com.zaphira.transaction.service.routing;

import com.zaphira.transaction.model.Transaction;
import com.zaphira.transaction.model.enums.PaymentMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Routing strategy for bank transfer payments.
 * Used as fallback when wallet routing fails or for large transfers.
 */
@Slf4j
@Component
public class BankRoutingStrategy implements RoutingStrategy {

    private static final BigDecimal BANK_TRANSFER_THRESHOLD = BigDecimal.valueOf(50000);

    @Override
    public boolean supports(PaymentMethod paymentMethod) {
        return paymentMethod == PaymentMethod.BANK_TRANSFER;
    }

    @Override
    public PaymentMethod route(Transaction transaction) {
        log.debug("[BANK_ROUTING] Routing transaction {} via bank transfer", transaction.getReference());

        // Check if bank routing is available
        if (isBankAvailable(transaction)) {
            return PaymentMethod.BANK_TRANSFER;
        }

        log.warn("[BANK_ROUTING] Bank gateway unavailable for {}", transaction.getReference());
        return null;
    }

    @Override
    public PaymentMethod getFallback(PaymentMethod preferred) {
        // If bank fails, try card network next
        return PaymentMethod.CARD;
    }

    @Override
    public RoutingValidationResult validate(Transaction transaction, PaymentMethod paymentMethod) {
        if (!supports(paymentMethod)) {
            return RoutingValidationResult.failure("UNSUPPORTED_METHOD", "Bank routing does not support " + paymentMethod);
        }

        // Check transaction amount is appropriate for bank transfer
        if (transaction.getAmount() != null && 
            transaction.getAmount().compareTo(BANK_TRANSFER_THRESHOLD) > 0) {
            return RoutingValidationResult.failure("AMOUNT_TOO_LARGE", "Amount exceeds bank transfer limit");
        }

        // Check receiver bank account exists
        if (transaction.getReceiverWallet() == null) {
            return RoutingValidationResult.failure("NO_RECEIVER_ACCOUNT", "Receiver bank account not configured");
        }

        return RoutingValidationResult.success();
    }

    @Override
    public int getPriority() {
        return PaymentMethod.BANK_TRANSFER.getPriority();
    }

    private boolean isBankAvailable(Transaction transaction) {
        // In production, check bank gateway availability
        // For now, assume bank is available if amount is within limits
        return transaction.getAmount() != null && 
               transaction.getAmount().compareTo(BANK_TRANSFER_THRESHOLD) <= 0;
    }
}
