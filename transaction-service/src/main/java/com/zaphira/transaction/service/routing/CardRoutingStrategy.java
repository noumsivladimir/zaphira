package com.zaphira.transaction.service.routing;

import com.zaphira.transaction.model.entities.Transaction;
import com.zaphira.transaction.model.enums.PaymentMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Routing strategy for card network payments.
 * Used as fallback when wallet and bank routing fail.
 */
@Slf4j
@Component
public class CardRoutingStrategy implements RoutingStrategy {

    @Override
    public boolean supports(PaymentMethod paymentMethod) {
        return paymentMethod == PaymentMethod.CARD;
    }

    @Override
    public PaymentMethod route(Transaction transaction) {
        log.debug("[CARD_ROUTING] Routing transaction {} via card network", transaction.getReference());

        // Check if card network is available
        if (isCardNetworkAvailable(transaction)) {
            return PaymentMethod.CARD;
        }

        log.warn("[CARD_ROUTING] Card network unavailable for {}", transaction.getReference());
        return null;
    }

    @Override
    public PaymentMethod getFallback(PaymentMethod preferred) {
        // If card fails, try mobile money next
        return PaymentMethod.MOBILE_MONEY;
    }

    @Override
    public RoutingValidationResult validate(Transaction transaction, PaymentMethod paymentMethod) {
        if (!supports(paymentMethod)) {
            return RoutingValidationResult.failure("UNSUPPORTED_METHOD", "Card routing does not support " + paymentMethod);
        }

        // Check receiver has card details
        if (transaction.getReceiverWalletId() == null) {
            return RoutingValidationResult.failure("NO_RECEIVER_CARD", "Receiver card details not available");
        }

        // Check card is not expired or frozen
        if (!Boolean.TRUE.equals(transaction.getReceiverWalletId())) {
            return RoutingValidationResult.failure("CARD_FROZEN", "Receiver card is frozen");
        }

        return RoutingValidationResult.success();
    }

    @Override
    public int getPriority() {
        return PaymentMethod.CARD.getPriority();
    }

    private boolean isCardNetworkAvailable(Transaction transaction) {
        // In production, check card network availability via Visa/Mastercard APIs
        // For now, assume card network is available
        return true;
    }
}
