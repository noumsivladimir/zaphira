package com.zaphira.transaction.controller;

import com.zaphira.transaction.dto.ApiResponse;
import com.zaphira.transaction.dto.AuthorizationInfoResponse;
import com.zaphira.transaction.dto.AuthorizationValidationRequest;
import com.zaphira.transaction.dto.TransactionHistoryResponseDTO;
import com.zaphira.transaction.dto.TransactionRequest;
import com.zaphira.transaction.dto.TransactionResponseDTO;
import com.zaphira.transaction.dto.UpdateStatusRequest;
import com.zaphira.transaction.dto.mapper.TransactionDTOConverterUtil;
import com.zaphira.transaction.model.Transaction;
import com.zaphira.transaction.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST Controller pour la gestion des transactions
 * Expose les endpoints pour créer, récupérer et mettre à jour les transactions
 * Retourne les réponses au format JSON sérialisable (DTOs) sans problèmes de lazy loading
 */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionDTOConverterUtil converterUtil;

    public TransactionController(TransactionService transactionService, TransactionDTOConverterUtil converterUtil) {
        this.transactionService = transactionService;
        this.converterUtil = converterUtil;
    }

    /**
     * Crée une nouvelle transaction
     * @param request les détails de la transaction à créer
     * @return la réponse avec la transaction créée
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TransactionResponseDTO>> createTransaction(@Valid @RequestBody TransactionRequest request) {
        Transaction saved = transactionService.createTransaction(request);
        TransactionResponseDTO responseDTO = converterUtil.toResponseDTO(saved);
        ApiResponse<TransactionResponseDTO> response = ApiResponse.success(responseDTO, "Transaction created successfully");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Récupère toutes les transactions
     * @return la liste de toutes les transactions
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TransactionResponseDTO>>> getAllTransactions() {
        List<Transaction> transactions = transactionService.getAllTransactions();
        List<TransactionResponseDTO> responseDTOs = converterUtil.toResponseDTOList(transactions);
        ApiResponse<List<TransactionResponseDTO>> response = ApiResponse.success(responseDTOs, "Transactions retrieved successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Récupère une transaction par son ID
     * @param id l'ID de la transaction
     * @return la transaction demandée
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponseDTO>> getTransaction(@PathVariable Long id) {
        Transaction tx = transactionService.getTransaction(id);
        if (tx == null) {
            ApiResponse<TransactionResponseDTO> response = ApiResponse.error("Transaction not found", "TRANSACTION_NOT_FOUND");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        TransactionResponseDTO responseDTO = converterUtil.toResponseDTO(tx);
        ApiResponse<TransactionResponseDTO> response = ApiResponse.success(responseDTO, "Transaction retrieved successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Récupère l'historique des états d'une transaction
     * @param id l'ID de la transaction
     * @return l'historique de la transaction
     */
    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse<TransactionHistoryResponseDTO>> getTransactionHistory(@PathVariable Long id) {
        Transaction tx = transactionService.getTransaction(id);
        if (tx == null) {
            ApiResponse<TransactionHistoryResponseDTO> response = ApiResponse.error("Transaction not found", "TRANSACTION_NOT_FOUND");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        
        List<com.zaphira.transaction.model.TransactionStateHistory> histories = transactionService.getTransactionHistory(id);
        
        TransactionHistoryResponseDTO responseDTO = TransactionHistoryResponseDTO.builder()
                .transactionId(tx.getId())
                .reference(tx.getReference())
                .historyCount(histories != null ? histories.size() : 0)
                .history(converterUtil.toHistoryDTOList(histories))
                .retrievedAt(LocalDateTime.now())
                .build();
        
        ApiResponse<TransactionHistoryResponseDTO> response = ApiResponse.success(responseDTO, "Transaction history retrieved successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Récupère les informations d'autorisation d'une transaction
     * @param id l'ID de la transaction
     * @return les informations d'autorisation
     */
    @GetMapping("/{id}/authorization")
    public ResponseEntity<ApiResponse<AuthorizationInfoResponse>> getAuthorizationInfo(@PathVariable Long id) {
        AuthorizationInfoResponse response = transactionService.getAuthorizationInfo(id);
        if (response == null) {
            ApiResponse<AuthorizationInfoResponse> apiResponse = ApiResponse.error("Transaction not found", "TRANSACTION_NOT_FOUND");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponse);
        }
        ApiResponse<AuthorizationInfoResponse> apiResponse = ApiResponse.success(response, "Authorization info retrieved successfully");
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Autorise une transaction
     * @param id l'ID de la transaction
     * @param request les détails d'autorisation
     * @return la transaction autorisée
     */
    @PostMapping("/{id}/authorize")
    public ResponseEntity<ApiResponse<TransactionResponseDTO>> authorizeTransaction(@PathVariable Long id,
                                                                                     @Valid @RequestBody AuthorizationValidationRequest request) {
        Transaction tx = transactionService.authorizeTransaction(id, request);
        if (tx == null) {
            ApiResponse<TransactionResponseDTO> response = ApiResponse.error("Transaction not found or authorization failed", "AUTHORIZATION_FAILED");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        TransactionResponseDTO responseDTO = converterUtil.toResponseDTO(tx);
        ApiResponse<TransactionResponseDTO> response = ApiResponse.success(responseDTO, "Transaction authorized successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Met à jour le statut d'une transaction
     * @param id l'ID de la transaction
     * @param request les nouveaux détails de statut
     * @return la transaction mise à jour
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<TransactionResponseDTO>> updateStatus(@PathVariable Long id,
                                                                             @Valid @RequestBody UpdateStatusRequest request) {
        Transaction updated = transactionService.updateTransactionStatus(id, request);
        if (updated == null) {
            ApiResponse<TransactionResponseDTO> response = ApiResponse.error("Transaction not found or status update failed", "STATUS_UPDATE_FAILED");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        TransactionResponseDTO responseDTO = converterUtil.toResponseDTO(updated);
        ApiResponse<TransactionResponseDTO> response = ApiResponse.success(responseDTO, "Transaction status updated successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Annule une transaction
     * @param id l'ID de la transaction
     * @param request les détails d'annulation
     * @return la transaction annulée
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<TransactionResponseDTO>> cancelTransaction(@PathVariable Long id,
                                                                                  @Valid @RequestBody UpdateStatusRequest request) {
        Transaction cancelled = transactionService.cancelTransaction(id, request);
        if (cancelled == null) {
            ApiResponse<TransactionResponseDTO> response = ApiResponse.error("Transaction not found or cancellation failed", "CANCELLATION_FAILED");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        TransactionResponseDTO responseDTO = converterUtil.toResponseDTO(cancelled);
        ApiResponse<TransactionResponseDTO> response = ApiResponse.success(responseDTO, "Transaction cancelled successfully");
        return ResponseEntity.ok(response);
    }
}


