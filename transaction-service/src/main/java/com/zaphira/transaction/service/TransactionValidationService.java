package com.zaphira.transaction.service;

import com.zaphira.transaction.dto.requests.TransactionRequest;
import com.zaphira.transaction.integration.wallet.WalletClient;
import com.zaphira.transaction.integration.wallet.dto.WalletDetailsResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Component
public class TransactionValidationService {

    private static final BigDecimal MIN_AMOUNT = BigDecimal.valueOf(0.01);

    private final WalletClient walletClient;

    public TransactionValidationService(WalletClient walletClient) {
        this.walletClient = walletClient;
    }

    public void validateInitiation(TransactionRequest request) {
        if (!StringUtils.hasText(request.getSenderWalletNumber())
                || !StringUtils.hasText(request.getReceiverWalletNumber())) {
            throw new IllegalArgumentException("Sender and receiver wallets must be provided");
        }

        if (request.getSenderWalletNumber().equals(request.getReceiverWalletNumber())) {
            throw new IllegalArgumentException("Sender and receiver wallets must be different");
        }

        if (request.getAmount() == null || request.getAmount().compareTo(MIN_AMOUNT) < 0) {
            throw new IllegalArgumentException("Amount must be greater than " + MIN_AMOUNT);
        }

        WalletDetailsResponse senderWallet = walletClient.getWalletDetails(request.getSenderWalletNumber());
        WalletDetailsResponse receiverWallet = walletClient.getWalletDetails(request.getReceiverWalletNumber());

        if (senderWallet.isFrozen()) {
            throw new IllegalArgumentException("Sender wallet is frozen");
        }

        if (receiverWallet.isFrozen()) {
            throw new IllegalArgumentException("Receiver wallet is frozen");
        }

        if (senderWallet.getAvailableBalance().compareTo(request.getAmount()) < 0) {
            throw new IllegalArgumentException("Insufficient balance in sender wallet");
        }
    }
}


