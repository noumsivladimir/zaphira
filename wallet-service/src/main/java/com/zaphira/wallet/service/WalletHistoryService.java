package com.zaphira.wallet.service;

import com.zaphira.wallet.dto.response.BalanceHistoryResponse;
import com.zaphira.wallet.dto.response.WalletHistoryResponse;
import com.zaphira.wallet.dto.response.WalletStatementResponse;
import org.springframework.core.io.Resource;

import java.time.LocalDateTime;

/**
 * Service interface for wallet history and statement operations
 */
public interface WalletHistoryService {
    
    /**
     * Get transaction history for a wallet with pagination
     * @param walletNumber Wallet number
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param startDate Optional start date filter
     * @param endDate Optional end date filter
     * @return Paginated transaction history
     */
    WalletHistoryResponse getWalletHistory(String walletNumber, int page, int size, 
                                          LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Generate account statement for a wallet
     * @param walletNumber Wallet number
     * @param startDate Start date of statement period
     * @param endDate End date of statement period
     * @return Statement with transactions and balance summary
     */
    WalletStatementResponse generateStatement(String walletNumber, 
                                              LocalDateTime startDate, 
                                              LocalDateTime endDate);
    
    /**
     * Generate and download statement as PDF
     * @param walletNumber Wallet number
     * @param startDate Start date of statement period
     * @param endDate End date of statement period
     * @return PDF file resource
     */
    Resource downloadStatementPdf(String walletNumber, 
                                 LocalDateTime startDate, 
                                 LocalDateTime endDate);
    
    /**
     * Get balance history over time
     * @param walletNumber Wallet number
     * @param startDate Start date
     * @param endDate End date
     * @return Daily balance snapshots
     */
    BalanceHistoryResponse getBalanceHistory(String walletNumber, 
                                            LocalDateTime startDate, 
                                            LocalDateTime endDate);
}
