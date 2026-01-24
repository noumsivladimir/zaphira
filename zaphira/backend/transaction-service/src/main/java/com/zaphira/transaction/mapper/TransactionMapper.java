package com.zaphira.transaction.mapper;

import com.zaphira.transaction.dto.TransactionRequest;
import com.zaphira.transaction.dto.TransactionResponse;
import com.zaphira.transaction.model.Transaction;
import org.mapstruct.*;

import java.util.List;

/**
 * MapStruct mapper for Transaction entity
 * 
 * @author Zaphira Platform
 * @version 2.0
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TransactionMapper {
    
    /**
     * Map Transaction entity to Response DTO
     */
    TransactionResponse toResponse(Transaction transaction);
    
    /**
     * Map list of Transaction entities to Response DTOs
     */
    List<TransactionResponse> toResponseList(List<Transaction> transactions);
    
    /**
     * Map Request DTO to Transaction entity
     * Note: Many fields are set by service logic
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    Transaction toEntity(TransactionRequest request);
}
