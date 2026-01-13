package com.zaphira.transaction.client;

import com.zaphira.common.dto.WalletDTO;
import com.zaphira.common.dto.WalletSummaryDTO;
import com.zaphira.common.dto.request.BalanceOperationRequest;
import com.zaphira.transaction.dto.requests.TransactionValidationRequest;
import com.zaphira.transaction.dto.response.TransactionValidationResponse;
import com.zaphira.transaction.exception.TransactionExceptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class WalletServiceClientFallbackFactory implements FallbackFactory<WalletServiceClient> {

    @Override
    public WalletServiceClient create(Throwable cause) {
        log.error("Wallet service fallback activated", cause);

        return new WalletServiceClient() {

//            @Override
            public TransactionValidationResponse validateTransaction(TransactionValidationRequest request) {
                log.error("Fallback: Cannot validate transaction for wallet {}: {}",
                        request.getWalletNumber(), cause.getMessage());
                throw new TransactionExceptions.WalletServiceException("Wallet service unavailable for validation", cause);
            }

            @Override
            public WalletSummaryDTO getWalletSummaryByWalletNumber(String walletNumber) {
                log.error("Wallet service fallback activated", cause);
                throw new TransactionExceptions.WalletServiceException("Wallet service unavailable for validation", cause);
            }

            @Override
            public void blockAmount(Long walletId, BalanceOperationRequest request) {
                log.error("Fallback: Cannot block amount on wallet {}: {}",
                        walletId, cause.getMessage());
                throw new TransactionExceptions.WalletServiceException("Wallet service unavailable for blocking amount", cause);
            }

            @Override
            public void unblockAmount(Long walletId, BalanceOperationRequest request) {
                log.error("Fallback: Cannot unblock amount on wallet {}: {}",
                        walletId, cause.getMessage());
                throw new TransactionExceptions.WalletServiceException("Wallet service unavailable for unblocking amount", cause);
            }

            @Override
            public void releaseBlockedAmount(Long walletId, BalanceOperationRequest request) {
                log.error("Fallback: Cannot release blocked amount on wallet {}: {}",
                        walletId, cause.getMessage());
                throw new TransactionExceptions.WalletServiceException("Wallet service unavailable for releasing amount", cause);
            }

            @Override
            public void creditWallet(Long walletId, BalanceOperationRequest request) {
                log.error("Fallback: Cannot credit wallet {}: {}", walletId, cause.getMessage());
                throw new TransactionExceptions.WalletServiceException("Wallet service unavailable for credit", cause);
            }

            @Override
            public void debitWallet(Long walletId, BalanceOperationRequest request) {
                log.error("Fallback: Cannot debit wallet {}: {}", walletId, cause.getMessage());
                throw new TransactionExceptions.WalletServiceException("Wallet service unavailable for debit", cause);
            }

            @Override
            public WalletDTO getWallet(Long walletId) {
                log.error("Fallback: Cannot get wallet {}: {}", walletId, cause.getMessage());
                throw new TransactionExceptions.WalletServiceException("Wallet service unavailable", cause);
            }
        };
    }
}