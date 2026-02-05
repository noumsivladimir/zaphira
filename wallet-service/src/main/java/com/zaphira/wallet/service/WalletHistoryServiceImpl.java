package com.zaphira.wallet.service;

import com.zaphira.common.dto.TransactionDTO;

import com.zaphira.wallet.dto.response.BalanceHistoryResponse;
import com.zaphira.wallet.dto.response.WalletHistoryResponse;
import com.zaphira.wallet.dto.response.WalletStatementResponse;
import com.zaphira.wallet.model.entities.Wallet;
import com.zaphira.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of WalletHistoryService for transaction history and statement operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletHistoryServiceImpl implements WalletHistoryService {

    private final WalletRepository walletRepository;
    private final RestTemplate restTemplate;
    private static final String TRANSACTION_SERVICE_URL = "http://transaction-service/api/v1/transactions";

    @Override
    @Transactional(readOnly = true)
    public WalletHistoryResponse getWalletHistory(String walletNumber, int page, int size,
                                                   LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Fetching history for wallet: {} (page: {}, size: {})", walletNumber, page, size);
        
        // Validate wallet exists
        Wallet wallet = walletRepository.findByWalletNumber(walletNumber)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found: " + walletNumber));
        
        try {
            // Build URL with pagination and filters
            StringBuilder urlBuilder = new StringBuilder(TRANSACTION_SERVICE_URL)
                    .append("/wallet/").append(wallet.getId())
                    .append("?page=").append(page)
                    .append("&size=").append(size);
            
            if (startDate != null) {
                urlBuilder.append("&startDate=").append(startDate);
            }
            if (endDate != null) {
                urlBuilder.append("&endDate=").append(endDate);
            }
            
            // Call transaction service to get transactions
            // In a real implementation, this would use a proper pagination response from transaction-service
            TransactionDTO[] transactions = restTemplate.getForObject(urlBuilder.toString(), TransactionDTO[].class);
            
            List<TransactionDTO> transactionList = transactions != null ? 
                    Arrays.asList(transactions) : new ArrayList<>();
            
            // Calculate pagination info (simplified - would come from transaction service in real implementation)
            int totalElements = transactionList.size();
            int totalPages = (totalElements + size - 1) / size;
            
            return WalletHistoryResponse.builder()
                    .walletNumber(walletNumber)
                    .transactions(transactionList)
                    .currentPage(page)
                    .totalPages(totalPages)
                    .totalElements(totalElements)
                    .pageSize(size)
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to fetch history for wallet {}: {}", walletNumber, e.getMessage());
            // Return empty history instead of throwing
            return WalletHistoryResponse.builder()
                    .walletNumber(walletNumber)
                    .transactions(new ArrayList<>())
                    .currentPage(page)
                    .totalPages(0)
                    .totalElements(0)
                    .pageSize(size)
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public WalletStatementResponse generateStatement(String walletNumber,
                                                     LocalDateTime startDate,
                                                     LocalDateTime endDate) {
        log.info("Generating statement for wallet: {} from {} to {}", walletNumber, startDate, endDate);
        
        // Validate wallet exists
        Wallet wallet = walletRepository.findByWalletNumber(walletNumber)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found: " + walletNumber));
        
        try {
            // Fetch all transactions for the period
            String url = TRANSACTION_SERVICE_URL + "/wallet/" + wallet.getId() +
                    "?startDate=" + startDate + "&endDate=" + endDate;
            
            TransactionDTO[] transactions = restTemplate.getForObject(url, TransactionDTO[].class);
            List<TransactionDTO> transactionList = transactions != null ? 
                    Arrays.asList(transactions) : new ArrayList<>();
            
            // Calculate totals
            BigDecimal totalCredits = transactionList.stream()
                    .filter(t -> t.getReceiverWalletNumber() != null && 
                                t.getReceiverWalletNumber().equals(walletNumber))
                    .map(TransactionDTO::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal totalDebits = transactionList.stream()
                    .filter(t -> t.getSenderWalletNumber() != null && 
                                t.getSenderWalletNumber().equals(walletNumber))
                    .map(TransactionDTO::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Opening balance would be: current balance - (credits - debits)
            BigDecimal openingBalance = wallet.getAvailableBalance()
                    .subtract(totalCredits)
                    .add(totalDebits);
            
            return WalletStatementResponse.builder()
                    .walletNumber(walletNumber)
                    .ownerName("User-" + wallet.getUserId()) // Would fetch from user service in real implementation
                    .currency("XOF") // Default currency - would be stored in wallet in real implementation
                    .openingBalance(openingBalance)
                    .closingBalance(wallet.getAvailableBalance())
                    .totalCredits(totalCredits)
                    .totalDebits(totalDebits)
                    .periodStart(startDate)
                    .periodEnd(endDate)
                    .generatedAt(LocalDateTime.now())
                    .transactions(transactionList)
                    .transactionCount(transactionList.size())
                    .statementId("STMT-" + walletNumber + "-" + System.currentTimeMillis())
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to generate statement for wallet {}: {}", walletNumber, e.getMessage());
            throw new RuntimeException("Failed to generate statement: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadStatementPdf(String walletNumber,
                                        LocalDateTime startDate,
                                        LocalDateTime endDate) {
        log.info("Generating PDF statement for wallet: {}", walletNumber);
        
        // Generate statement data
        WalletStatementResponse statement = generateStatement(walletNumber, startDate, endDate);
        
        // Generate simple text-based PDF content
        // In a real implementation, this would use a PDF library like iText or Apache PDFBox
        StringBuilder pdfContent = new StringBuilder();
        pdfContent.append("=== WALLET STATEMENT ===\n\n");
        pdfContent.append("Statement ID: ").append(statement.getStatementId()).append("\n");
        pdfContent.append("Wallet Number: ").append(statement.getWalletNumber()).append("\n");
        pdfContent.append("Owner: ").append(statement.getOwnerName()).append("\n");
        pdfContent.append("Currency: ").append(statement.getCurrency()).append("\n\n");
        
        pdfContent.append("Period: ").append(startDate).append(" to ").append(endDate).append("\n\n");
        
        pdfContent.append("Opening Balance: ").append(statement.getOpeningBalance()).append("\n");
        pdfContent.append("Total Credits: ").append(statement.getTotalCredits()).append("\n");
        pdfContent.append("Total Debits: ").append(statement.getTotalDebits()).append("\n");
        pdfContent.append("Closing Balance: ").append(statement.getClosingBalance()).append("\n\n");
        
        pdfContent.append("=== TRANSACTIONS (").append(statement.getTransactionCount()).append(") ===\n\n");
        
        for (TransactionDTO txn : statement.getTransactions()) {
            pdfContent.append("Date: ").append(txn.getCreatedAt()).append("\n");
            pdfContent.append("Reference: ").append(txn.getReference()).append("\n");
            pdfContent.append("Type: ").append(txn.getType()).append("\n");
            pdfContent.append("Amount: ").append(txn.getAmount()).append(" ").append(txn.getCurrency()).append("\n");
            pdfContent.append("Status: ").append(txn.getStatus()).append("\n");
            pdfContent.append("---\n");
        }
        
        pdfContent.append("\nGenerated at: ").append(statement.getGeneratedAt()).append("\n");
        
        byte[] pdfBytes = pdfContent.toString().getBytes();
        return new ByteArrayResource(pdfBytes);
    }

    @Override
    @Transactional(readOnly = true)
    public BalanceHistoryResponse getBalanceHistory(String walletNumber,
                                                    LocalDateTime startDate,
                                                    LocalDateTime endDate) {
        log.info("Fetching balance history for wallet: {} from {} to {}", walletNumber, startDate, endDate);
        
        // Validate wallet exists
        Wallet wallet = walletRepository.findByWalletNumber(walletNumber)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found: " + walletNumber));
        
        try {
            // Fetch transactions for the period
            String url = TRANSACTION_SERVICE_URL + "/wallet/" + wallet.getId() +
                    "?startDate=" + startDate + "&endDate=" + endDate;
            
            TransactionDTO[] transactions = restTemplate.getForObject(url, TransactionDTO[].class);
            List<TransactionDTO> transactionList = transactions != null ? 
                    Arrays.asList(transactions) : new ArrayList<>();
            
            // Group transactions by date and calculate daily balances
            Map<LocalDate, List<TransactionDTO>> transactionsByDate = transactionList.stream()
                    .collect(Collectors.groupingBy(t -> t.getCreatedAt().toLocalDate()));
            
            // Calculate balance snapshots
            List<BalanceHistoryResponse.BalanceSnapshot> snapshots = new ArrayList<>();
            BigDecimal runningBalance = wallet.getAvailableBalance();
            
            // Work backwards from current balance
            LocalDate currentDate = endDate.toLocalDate();
            LocalDate startLocalDate = startDate.toLocalDate();
            
            while (!currentDate.isBefore(startLocalDate)) {
                List<TransactionDTO> dailyTransactions = transactionsByDate.getOrDefault(currentDate, new ArrayList<>());
                
                // Calculate daily net change
                BigDecimal dailyCredits = dailyTransactions.stream()
                        .filter(t -> t.getReceiverWalletNumber() != null && 
                                    t.getReceiverWalletNumber().equals(walletNumber))
                        .map(TransactionDTO::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                BigDecimal dailyDebits = dailyTransactions.stream()
                        .filter(t -> t.getSenderWalletNumber() != null && 
                                    t.getSenderWalletNumber().equals(walletNumber))
                        .map(TransactionDTO::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                BalanceHistoryResponse.BalanceSnapshot snapshot = BalanceHistoryResponse.BalanceSnapshot.builder()
                        .date(currentDate)
                        .availableBalance(runningBalance)
                        .blockedBalance(wallet.getBlockedBalance()) // Simplified - would track historical blocked amount
                        .totalBalance(runningBalance.add(wallet.getBlockedBalance()))
                        .transactionCount(dailyTransactions.size())
                        .build();
                
                snapshots.add(0, snapshot); // Add to beginning for chronological order
                
                // Adjust balance for previous day
                runningBalance = runningBalance.subtract(dailyCredits).add(dailyDebits);
                currentDate = currentDate.minusDays(1);
            }
            
            return BalanceHistoryResponse.builder()
                    .walletNumber(walletNumber)
                    .startDate(startDate)
                    .endDate(endDate)
                    .balanceSnapshots(snapshots)
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to fetch balance history for wallet {}: {}", walletNumber, e.getMessage());
            return BalanceHistoryResponse.builder()
                    .walletNumber(walletNumber)
                    .startDate(startDate)
                    .endDate(endDate)
                    .balanceSnapshots(new ArrayList<>())
                    .build();
        }
    }
}
