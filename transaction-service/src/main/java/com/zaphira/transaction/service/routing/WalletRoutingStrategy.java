package com.zaphira.transaction.service.routing;

import com.zaphira.transaction.model.Transaction;
import com.zaphira.transaction.model.enums.PaymentMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Routing strategy for internal wallet-to-wallet transfers.
 * This is the default and most efficient routing method.
 */
@Slf4j
@Component
public class WalletRoutingStrategy implements RoutingStrategy {

    @Override
    public boolean supports(PaymentMethod paymentMethod) {
        return paymentMethod == PaymentMethod.WALLET;
    }

    @Override
    public PaymentMethod route(Transaction transaction) {
        log.debug("[WALLET_ROUTING] Routing transaction {} via wallet", transaction.getReference());
        
        // Wallet-to-wallet is always available for internal transfers
        if (isBothWalletsActive(transaction)) {
            return PaymentMethod.WALLET;
        }
        
        log.warn("[WALLET_ROUTING] Cannot route via wallet: inactive wallets for {}", transaction.getReference());
        return null;
    }

    @Override
    public PaymentMethod getFallback(PaymentMethod preferred) {
        // If wallet fails, try bank transfer next
        return PaymentMethod.BANK_TRANSFER;
    }

    @Override
    public RoutingValidationResult validate(Transaction transaction, PaymentMethod paymentMethod) {
        if (!supports(paymentMethod)) {
            return RoutingValidationResult.failure("UNSUPPORTED_METHOD", "Wallet routing does not support " + paymentMethod);
        }

        // Check both wallets are present and active
        if (transaction.getSenderWallet() == null) {
            return RoutingValidationResult.failure("NO_SENDER_WALLET", "Sender wallet not found");
        }

        if (transaction.getReceiverWallet() == null) {
            return RoutingValidationResult.failure("NO_RECEIVER_WALLET", "Receiver wallet not found");
        }

        // Check sender wallet has sufficient balance (availableBalance is used for transaction validation)
        if (transaction.getAmount() != null && 
            transaction.getSenderWallet().getAvailableBalance() != null &&
            transaction.getAmount().compareTo(transaction.getSenderWallet().getAvailableBalance()) > 0) {
            return RoutingValidationResult.failure("INSUFFICIENT_BALANCE", "Sender wallet has insufficient balance");
        }

        // Check both wallets are active
        if (!isBothWalletsActive(transaction)) {
            return RoutingValidationResult.failure("INACTIVE_WALLET", "One or both wallets are inactive");
        }

        return RoutingValidationResult.success();
    }

    @Override
    public int getPriority() {
        return PaymentMethod.WALLET.getPriority();
    }

    private boolean isBothWalletsActive(Transaction transaction) {
        return transaction.getSenderWallet() != null && 
               transaction.getReceiverWallet() != null &&
               Boolean.TRUE.equals(transaction.getSenderWallet().getActive()) &&
               Boolean.TRUE.equals(transaction.getReceiverWallet().getActive());
    }
}
