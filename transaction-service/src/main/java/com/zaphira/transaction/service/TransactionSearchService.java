//package com.zaphira.transaction.service;
//
//import com.zaphira.transaction.dto.TransactionExportDTO;
//import com.zaphira.transaction.dto.requests.TransactionSearchRequest;
//import com.zaphira.transaction.model.entities.Transaction;
//import com.zaphira.transaction.repository.TransactionRepository;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.io.*;
//import java.nio.charset.StandardCharsets;
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.stream.Collectors;
//
///**
// * Service for advanced transaction search and filtering.
// * Provides comprehensive search capabilities with multiple filter combinations.
// * Supports pagination, sorting, and export functionality.
// */
//@Slf4j
//@Service
//@Transactional(readOnly = true)
//public class TransactionSearchService {
//
//    private final TransactionRepository transactionRepository;
//
//    public TransactionSearchService(TransactionRepository transactionRepository) {
//        this.transactionRepository = transactionRepository;
//    }
//
//    /**
//     * Search transactions with advanced filtering.
//     *
//     * @param searchRequest Search criteria
//     * @param pageable      Pagination and sorting information
//     * @return Paginated search results
//     */
//    public Page<Transaction> search(TransactionSearchRequest searchRequest, Pageable pageable) {
//        log.debug("Searching transactions with criteria: {}", searchRequest);
//
//        // Validate search request
//        if (searchRequest == null) {
//            log.warn("Search request is null, returning all transactions");
//            return transactionRepository.findAll(pageable);
//        }
//
//        // Use custom query with all filter criteria
//        Page<Transaction> results = transactionRepository.searchTransactions(
//                searchRequest.getSenderWalletNumber(),
//                searchRequest.getReceiverWalletNumber(),
//                searchRequest.getStatus(),
//                searchRequest.getType(),
//                searchRequest.getMinAmount(),
//                searchRequest.getMaxAmount(),
//                searchRequest.getStartDate(),
//                searchRequest.getEndDate(),
//                searchRequest.getTransactionReference(),
//                searchRequest.getIsScheduled(),
//                pageable
//        );
//
//        return results;
//    }
//
//    /**
//     * Search by sender wallet.
//     *
//     * @param senderWalletNumber Sender wallet number
//     * @param pageable           Pagination info
//     * @return Paginated results
//     */
//    public Page<Transaction> searchBySender(String senderWalletNumber, Pageable pageable) {
//        log.debug("Searching transactions by sender wallet: {}", senderWalletNumber);
//        return transactionRepository.findBySenderWalletNumber(senderWalletNumber, pageable);
//    }
//
//    /**
//     * Search by receiver wallet.
//     *
//     * @param receiverWalletNumber Receiver wallet number
//     * @param pageable             Pagination info
//     * @return Paginated results
//     */
//    public Page<Transaction> searchByReceiver(String receiverWalletNumber, Pageable pageable) {
//        log.debug("Searching transactions by receiver wallet: {}", receiverWalletNumber);
//        return transactionRepository.findByReceiverWalletNumber(receiverWalletNumber, pageable);
//    }
//
//    /**
//     * Search by amount range.
//     *
//     * @param minAmount Minimum amount (inclusive)
//     * @param maxAmount Maximum amount (inclusive)
//     * @param pageable  Pagination info
//     * @return Paginated results
//     */
//    public Page<Transaction> searchByAmountRange(java.math.BigDecimal minAmount, java.math.BigDecimal maxAmount, Pageable pageable) {
//        log.debug("Searching transactions by amount range: {} to {}", minAmount, maxAmount);
//        return transactionRepository.findByAmountBetween(minAmount, maxAmount, pageable);
//    }
//
//    /**
//     * Search by status and date range.
//     *
//     * @param status   Transaction status
//     * @param fromDate Start date (inclusive)
//     * @param toDate   End date (inclusive)
//     * @param pageable Pagination info
//     * @return Paginated results
//     */
//    public Page<Transaction> searchByStatusAndDate(
//            com.zaphira.transaction.model.enums.TransactionStatus status,
//            LocalDateTime fromDate,
//            LocalDateTime toDate,
//            Pageable pageable) {
//        log.debug("Searching transactions by status: {} from {} to {}", status, fromDate, toDate);
//        return transactionRepository.findByStatusAndCreatedAtBetween(status, fromDate, toDate, pageable);
//    }
//
//    /**
//     * Search by type and currency.
//     *
//     * @param type     Transaction type
//     * @param currency Currency code
//     * @param pageable Pagination info
//     * @return Paginated results
//     */
//    public Page<Transaction> searchByTypeAndCurrency(
//            com.zaphira.transaction.model.enums.TransactionType type,
//            String currency,
//            Pageable pageable) {
//        log.debug("Searching transactions by type: {} and currency: {}", type, currency);
//        return transactionRepository.findByTypeAndCurrency(type, currency, pageable);
//    }
//
//    /**
//     * Export transactions to CSV format.
//     *
//     * @param transactions Transactions to export
//     * @return CSV content as ByteArrayResource
//     */
//    public org.springframework.core.io.ByteArrayResource exportToCSV(List<Transaction> transactions) {
//        log.debug("Exporting {} transactions to CSV", transactions.size());
//
//        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
//             OutputStreamWriter writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
//             BufferedWriter bw = new BufferedWriter(writer)) {
//
//            // Write CSV header
//            bw.write("ID,Reference,Sender Wallet,Receiver Wallet,Amount,Currency,Fee,Status,Type,Route,Created At,Completed At,Risk Score");
//            bw.newLine();
//
//            // Write transaction data
//            for (Transaction tx : transactions) {
//                String line = String.format(
//                        "%d,\"%s\",\"%s\",\"%s\",%s,%s,%s,%s,%s,%s,%s,%s,%d",
//                        tx.getId(),
//                        tx.getReference(),
//                        tx.getSenderWalletNumber(),
//                        tx.getReceiverWalletNumber(),
//                        tx.getAmount(),
//                        tx.getCurrency(),
//                        tx.getFeeAmount() != null ? tx.getFeeAmount() : "0",
//                        tx.getStatus(),
//                        tx.getType(),
//                        tx.getRoute() != null ? tx.getRoute() : "",
//                        tx.getCreatedAt(),
//                        tx.getCompletedAt() != null ? tx.getCompletedAt() : "",
//                        tx.getRiskScore() != null ? tx.getRiskScore() : 0
//                );
//                bw.write(line);
//                bw.newLine();
//            }
//
//            bw.flush();
//            return new org.springframework.core.io.ByteArrayResource(baos.toByteArray());
//
//        } catch (IOException e) {
//            log.error("Error exporting transactions to CSV", e);
//            throw new RuntimeException("Failed to export transactions to CSV", e);
//        }
//    }
//
//    /**
//     * Export transactions to JSON format.
//     *
//     * @param transactions Transactions to export
//     * @return JSON content as ByteArrayResource
//     */
//    public org.springframework.core.io.ByteArrayResource exportToJSON(List<Transaction> transactions) {
//        log.debug("Exporting {} transactions to JSON", transactions.size());
//
//        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
//             OutputStreamWriter writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8)) {
//
//            // Convert to DTOs
//            List<TransactionExportDTO> dtos = transactions.stream()
//                    .map(this::convertToDTO)
//                    .collect(Collectors.toList());
//
//            // Use Jackson ObjectMapper for JSON serialization
//            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
//            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
//            mapper.writeValue(writer, dtos);
//
//            return new org.springframework.core.io.ByteArrayResource(baos.toByteArray());
//
//        } catch (IOException e) {
//            log.error("Error exporting transactions to JSON", e);
//            throw new RuntimeException("Failed to export transactions to JSON", e);
//        }
//    }
//
//    /**
//     * Convert Transaction entity to ExportDTO.
//     *
//     * @param transaction Source transaction
//     * @return Export DTO
//     */
//    private TransactionExportDTO convertToDTO(Transaction transaction) {
//        return TransactionExportDTO.builder()
//                .id(transaction.getId())
//                .reference(transaction.getReference())
//                .senderWalletNumber(transaction.getSenderWalletNumber())
//                .receiverWalletNumber(transaction.getReceiverWalletNumber())
//                .amount(transaction.getAmount())
//                .currency(transaction.getCurrency())
//                .feeAmount(transaction.getFeeAmount())
//                .feeCurrency(transaction.getFeeCurrency())
//                .type(transaction.getType())
//                .status(transaction.getStatus())
//                .route(transaction.getRoute())
//                .description(transaction.getDescription())
//                .riskScore(transaction.getRiskScore())
//                .createdAt(transaction.getCreatedAt())
//                .completedAt(transaction.getCompletedAt())
//                .failedAt(transaction.getFailedAt())
//                .cancelledAt(transaction.getCancelledAt())
//                .build();
//    }
//
//    /**
//     * Export transactions with filters and format.
//     *
//     * @param searchRequest Search criteria
//     * @param format        Export format (CSV or JSON)
//     * @param pageable      Pagination info
//     * @return Exported resource
//     */
//    public org.springframework.core.io.Resource exportTransactions(
//            TransactionSearchRequest searchRequest,
//            String format,
//            Pageable pageable) {
//        log.debug("Exporting transactions with format: {}", format);
//
//        // Get filtered transactions
//        Page<Transaction> results = search(searchRequest, pageable);
//        List<Transaction> transactions = results.getContent();
//
//        // Export based on format
//        if ("JSON".equalsIgnoreCase(format)) {
//            return exportToJSON(transactions);
//        } else {
//            return exportToCSV(transactions);
//        }
//    }
//
//    /**
//     * Get transaction count by status.
//     *
//     * @return Status statistics
//     */
//    public java.util.Map<String, Long> getStatusStatistics() {
//        log.debug("Calculating transaction status statistics");
//        return transactionRepository.findAll()
//                .stream()
//                .collect(java.util.stream.Collectors.groupingByConcurrent(
//                        tx -> tx.getStatus().toString(),
//                        java.util.stream.Collectors.counting()
//                ));
//    }
//}
