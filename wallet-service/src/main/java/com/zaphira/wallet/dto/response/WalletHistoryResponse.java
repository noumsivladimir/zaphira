package com.zaphira.wallet.dto.response;

import com.zaphira.common.dto.TransactionDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for wallet transaction history with pagination
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletHistoryResponse {
    private String walletNumber;
    private List<TransactionDTO> transactions;
    private int currentPage;
    private int totalPages;
    private long totalElements;
    private int pageSize;
}
