package com.zaphira.transaction.controller;

import com.zaphira.transaction.dto.TransactionDTO;
import com.zaphira.transaction.dto.response.MerchantReportDTO;
import com.zaphira.transaction.dto.response.MerchantAnalyticsDTO;
import com.zaphira.transaction.model.enums.TransactionStatus;
import com.zaphira.transaction.service.MerchantReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * LOT 4: Merchant Reports & Analytics
 * Provides merchants with insights into their sales and transactions
 */
@Slf4j
@RestController
@RequestMapping("/api/transactions/merchant")
@RequiredArgsConstructor
public class MerchantReportController {
    
    private final MerchantReportService merchantReportService;
    
    // LOT 4: Get Merchant Sales (transactions received by merchant)
    @GetMapping("/sales")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<Page<TransactionDTO>> getMerchantSales(
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        log.info("Fetching merchant sales with filters: status={}, from={}, to={}", status, from, to);
        Page<TransactionDTO> sales = merchantReportService.getMerchantSales(status, from, to, page, size);
        return ResponseEntity.ok(sales);
    }
    
    // LOT 4: Get Merchant Report (summary of sales)
    @GetMapping("/report")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<MerchantReportDTO> getMerchantReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        
        log.info("Generating merchant report from {} to {}", from, to);
        MerchantReportDTO report = merchantReportService.getMerchantReport(from, to);
        return ResponseEntity.ok(report);
    }
    
    // LOT 4: Get Merchant Analytics (aggregated stats)
    @GetMapping("/analytics")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<MerchantAnalyticsDTO> getMerchantAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        
        log.info("Fetching merchant analytics from {} to {}", from, to);
        MerchantAnalyticsDTO analytics = merchantReportService.getMerchantAnalytics(from, to);
        return ResponseEntity.ok(analytics);
    }
    
    // LOT 4: Get Merchant Settlements (transactions requiring settlement)
    @GetMapping("/settlements")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<Page<TransactionDTO>> getMerchantSettlements(
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        log.info("Fetching merchant settlements with status: {}", status);
        Page<TransactionDTO> settlements = merchantReportService.getMerchantSettlements(status, page, size);
        return ResponseEntity.ok(settlements);
    }
    
    // LOT 4: Get Merchant Refunds (refunds issued by merchant)
    @GetMapping("/refunds")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<Page<TransactionDTO>> getMerchantRefunds(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        log.info("Fetching merchant refunds from {} to {}", from, to);
        Page<TransactionDTO> refunds = merchantReportService.getMerchantRefunds(from, to, page, size);
        return ResponseEntity.ok(refunds);
    }
    
    // ADMIN: Get Merchant Report for any merchant
    @GetMapping("/{merchantId}/report")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MerchantReportDTO> getMerchantReportById(
            @PathVariable Long merchantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        
        log.info("ADMIN: Generating report for merchant {} from {} to {}", merchantId, from, to);
        MerchantReportDTO report = merchantReportService.getMerchantReportById(merchantId, from, to);
        return ResponseEntity.ok(report);
    }
}
