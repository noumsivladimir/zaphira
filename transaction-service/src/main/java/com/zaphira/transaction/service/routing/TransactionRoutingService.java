//package com.zaphira.transaction.service.routing;
//
//import com.zaphira.transaction.model.entities.Transaction;
//import com.zaphira.transaction.model.enums.PaymentMethod;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * Orchestrates transaction routing across multiple payment methods.
// * Implements Strategy pattern with automatic fallback mechanism.
// *
// * Flow:
// * 1. Determine preferred payment method
// * 2. Validate transaction for that method
// * 3. If validation fails, try fallback methods
// * 4. Return selected payment method or raise exception
// */
//@Slf4j
//@Service
//public class TransactionRoutingService {
//
//    private final List<RoutingStrategy> strategies;
//
//    public TransactionRoutingService(
//            WalletRoutingStrategy walletStrategy,
//            BankRoutingStrategy bankStrategy,
//            CardRoutingStrategy cardStrategy) {
//        this.strategies = new ArrayList<>();
//        this.strategies.add(walletStrategy);
//        this.strategies.add(bankStrategy);
//        this.strategies.add(cardStrategy);
//
//        // Sort by priority
//        this.strategies.sort((s1, s2) -> Integer.compare(s1.getPriority(), s2.getPriority()));
//    }
//
//    /**
//     * Route a transaction using intelligent fallback mechanism.
//     *
//     * @param transaction The transaction to route
//     * @return The selected payment method
//     * @throws RoutingException if no valid routing path exists
//     */
//    public PaymentMethod routeTransaction(Transaction transaction) {
//        log.info("[ROUTING] Starting routing for transaction {}", transaction.getReference());
//
//        // If payment method is already specified, validate and use it
//        if (transaction.getPaymentMethod() != null) {
//            return routeWithPreferredMethod(transaction, transaction.getPaymentMethod());
//        }
//
//        // Otherwise, try each strategy by priority
//        for (RoutingStrategy strategy : strategies) {
//            PaymentMethod result = tryRoute(transaction, strategy);
//            if (result != null) {
//                transaction.setPaymentMethod(result);
//                transaction.setRoute(result.getRoutingKey());
//                log.info("[ROUTING] Transaction {} routed to {}", transaction.getReference(), result);
//                return result;
//            }
//        }
//
//        // No valid routing path found
//        log.error("[ROUTING] No valid routing path for transaction {}", transaction.getReference());
//        throw new RoutingException("ROUTING_FAILED", "No valid routing path for transaction " + transaction.getReference());
//    }
//
//    /**
//     * Route with a specific preferred payment method, with fallbacks.
//     */
//    private PaymentMethod routeWithPreferredMethod(Transaction transaction, PaymentMethod preferred) {
//        log.debug("[ROUTING] Attempting preferred method: {}", preferred);
//
//        RoutingStrategy strategy = findStrategy(preferred);
//        if (strategy == null) {
//            log.warn("[ROUTING] No strategy found for {}", preferred);
//            throw new RoutingException("UNSUPPORTED_METHOD", "Unsupported payment method: " + preferred);
//        }
//
//        // Validate preferred method
//        RoutingValidationResult validation = strategy.validate(transaction, preferred);
//        if (validation.isValid()) {
//            transaction.setPaymentMethod(preferred);
//            transaction.setRoute(preferred.getRoutingKey());
//            log.info("[ROUTING] Preferred method {} validated successfully", preferred);
//            return preferred;
//        }
//
//        log.warn("[ROUTING] Preferred method {} failed: {}", preferred, validation.getMessage());
//
//        // Try fallback methods
//        PaymentMethod fallback = preferred.getNextFallback();
//        int attempts = 0;
//        int maxAttempts = 5; // Prevent infinite loops
//
//        while (fallback != null && attempts < maxAttempts) {
//            attempts++;
//            log.debug("[ROUTING] Trying fallback method: {} (attempt {})", fallback, attempts);
//
//            PaymentMethod result = routeWithPreferredMethod(transaction, fallback);
//            if (result != null) {
//                return result;
//            }
//
//            fallback = fallback.getNextFallback();
//        }
//
//        // No fallback succeeded
//        log.error("[ROUTING] All routing attempts failed for transaction {}", transaction.getReference());
//        throw new RoutingException("ALL_ROUTES_FAILED", "All routing methods failed for transaction " + transaction.getReference());
//    }
//
//    /**
//     * Try to route using a specific strategy.
//     */
//    private PaymentMethod tryRoute(Transaction transaction, RoutingStrategy strategy) {
//        try {
//            PaymentMethod method = strategy.route(transaction);
//            if (method != null) {
//                RoutingValidationResult validation = strategy.validate(transaction, method);
//                if (validation.isValid()) {
//                    return method;
//                }
//            }
//            return null;
//        } catch (Exception e) {
//            log.error("[ROUTING] Strategy {} failed with exception", strategy.getClass().getSimpleName(), e);
//            return null;
//        }
//    }
//
//    /**
//     * Validate a transaction for a specific payment method.
//     */
//    public RoutingValidationResult validateRoute(Transaction transaction, PaymentMethod paymentMethod) {
//        RoutingStrategy strategy = findStrategy(paymentMethod);
//        if (strategy == null) {
//            return RoutingValidationResult.failure("UNSUPPORTED_METHOD", "No strategy for " + paymentMethod);
//        }
//        return strategy.validate(transaction, paymentMethod);
//    }
//
//    /**
//     * Get the next fallback payment method.
//     */
//    public PaymentMethod getNextFallback(PaymentMethod current) {
//        return current.getNextFallback();
//    }
//
//    /**
//     * Find routing strategy for a payment method.
//     */
//    private RoutingStrategy findStrategy(PaymentMethod paymentMethod) {
//        return strategies.stream()
//                .filter(s -> s.supports(paymentMethod))
//                .findFirst()
//                .orElse(null);
//    }
//
//    /**
//     * Get all available payment methods.
//     */
//    public List<PaymentMethod> getAvailablePaymentMethods() {
//        return strategies.stream()
//                .map(s -> {
//                    for (PaymentMethod pm : PaymentMethod.values()) {
//                        if (s.supports(pm)) {
//                            return pm;
//                        }
//                    }
//                    return null;
//                })
//                .filter(java.util.Objects::nonNull)
//                .toList();
//    }
//}
