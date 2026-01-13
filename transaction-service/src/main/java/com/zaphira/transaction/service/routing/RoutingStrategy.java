package com.zaphira.transaction.service.routing;

import com.zaphira.transaction.model.entities.Transaction;
import com.zaphira.transaction.model.enums.PaymentMethod;

/**
 * Strategy interface for routing transactions to different payment methods.
 * Implementations handle specific routing logic for different methods.
 */
public interface RoutingStrategy {

    /**
     * Determine if this strategy can handle the given payment method.
     */
    boolean supports(PaymentMethod paymentMethod);

    /**
     * Route the transaction and return the selected payment method.
     * Returns null if routing fails.
     */
    PaymentMethod route(Transaction transaction);

    /**
     * Get a fallback payment method if the preferred one fails.
     */
    PaymentMethod getFallback(PaymentMethod preferred);

    /**
     * Validate if the transaction can be routed through this method.
     */
    RoutingValidationResult validate(Transaction transaction, PaymentMethod paymentMethod);

    /**
     * Get the routing priority for this strategy.
     */
    int getPriority();
}
