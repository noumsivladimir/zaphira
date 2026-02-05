package com.zaphira.transaction.mapper;

import com.zaphira.transaction.dto.TransactionDTO;
import com.zaphira.transaction.model.entities.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "reference", source = "reference")
    @Mapping(target = "totalAmount",
            expression = "java(transaction.getFeeAmount() == null ? transaction.getAmount() : transaction.getAmount().add(transaction.getFeeAmount()))"
    )
    @Mapping(target = "completedAt", source = "updatedAt")
    @Mapping(target = "subWalletId", ignore = true)
    @Mapping(target = "externalReference", ignore = true)
    @Mapping(target = "failureReason", source = "failureReason")
    @Mapping(target = "metadata", ignore = true) 
    TransactionDTO toDTO(Transaction transaction);

    List<TransactionDTO> toDTOList(List<Transaction> transactions);

    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "channel", ignore = true)
    @Mapping(target = "transactionReference", ignore = true)
    @Mapping(target = "scheduled", ignore = true)
    @Mapping(target = "scheduledFor", ignore = true)
    @Mapping(target = "isRefunded", ignore = true)
    @Mapping(target = "isReversed", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "lastUpdatedAt", ignore = true)
    @Mapping(target = "risk", ignore = true)
    @Mapping(target = "retry", ignore = true)
    @Mapping(target = "authorizationInfo", ignore = true)
    Transaction toEntity(TransactionDTO dto);
}