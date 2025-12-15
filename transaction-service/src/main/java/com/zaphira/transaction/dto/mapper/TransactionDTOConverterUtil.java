package com.zaphira.transaction.dto.mapper;

import com.zaphira.transaction.dto.AuthorizationInfoResponse;
import com.zaphira.transaction.dto.TransactionDTO;
import com.zaphira.transaction.dto.TransactionEventDTO;
import com.zaphira.transaction.dto.TransactionResponseDTO;
import com.zaphira.transaction.dto.TransactionStateHistoryDTO;
import com.zaphira.transaction.model.Transaction;
import com.zaphira.transaction.model.TransactionStateHistory;
import com.zaphira.transaction.model.enums.TransactionStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Classe utilitaire pour les conversions en batch
 * Gère la conversion de listes d'entités vers leurs DTOs correspondants
 */
@Component
public class TransactionDTOConverterUtil {

    private final TransactionDTOMapper mapper;

    public TransactionDTOConverterUtil(TransactionDTOMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Convertit une liste d'entités Transaction en liste de DTOs
     */
    public List<TransactionDTO> toDTOList(List<Transaction> transactions) {
        if (transactions == null) {
            return null;
        }
        return transactions.stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Convertit une liste d'entités Transaction en liste de TransactionResponseDTOs
     */
    public List<TransactionResponseDTO> toResponseDTOList(List<Transaction> transactions) {
        if (transactions == null) {
            return null;
        }
        return transactions.stream()
                .map(mapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Convertit une liste d'entités TransactionStateHistory en liste de DTOs
     */
    public List<TransactionStateHistoryDTO> toHistoryDTOList(List<TransactionStateHistory> histories) {
        if (histories == null) {
            return null;
        }
        return histories.stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Convertit une liste d'entités Transaction en liste d'AuthorizationInfoResponse
     */
    public List<AuthorizationInfoResponse> toAuthorizationInfoResponseList(List<Transaction> transactions) {
        if (transactions == null) {
            return null;
        }
        return transactions.stream()
                .map(mapper::toAuthorizationInfoResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convertit une liste d'entités Transaction en liste d'événements Kafka
     */
    public List<TransactionEventDTO> toEventDTOList(List<Transaction> transactions, String eventType) {
        if (transactions == null) {
            return null;
        }
        return transactions.stream()
                .map(tx -> mapper.toEventDTO(tx, eventType))
                .collect(Collectors.toList());
    }

    /**
     * Convertit une Transaction en TransactionResponseDTO
     */
    public TransactionResponseDTO toResponseDTO(Transaction transaction) {
        return mapper.toResponseDTO(transaction);
    }

    /**
     * Convertit une Transaction en AuthorizationInfoResponse
     */
    public AuthorizationInfoResponse toAuthorizationInfoResponse(Transaction transaction) {
        return mapper.toAuthorizationInfoResponse(transaction);
    }

    /**
     * Convertit une Transaction en TransactionEventDTO
     */
    public TransactionEventDTO toEventDTO(Transaction transaction, String eventType, TransactionStatus previousStatus) {
        return mapper.toEventDTO(transaction, eventType, previousStatus);
    }

    /**
     * Convertit une Transaction en TransactionEventDTO sans statut précédent
     */
    public TransactionEventDTO toEventDTO(Transaction transaction, String eventType) {
        return mapper.toEventDTO(transaction, eventType);
    }
}
