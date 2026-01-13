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
            expression = "java(transaction.getAmount().add(transaction.getFeeAmount() ))"
    )
    @Mapping(target = "completedAt", source = "updatedAt")
    TransactionDTO toDTO(Transaction transaction);

    List<TransactionDTO> toDTOList(List<Transaction> transactions);

    Transaction toEntity(TransactionDTO dto);
}