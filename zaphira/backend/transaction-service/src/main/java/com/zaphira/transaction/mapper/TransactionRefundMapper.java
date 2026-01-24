package com.zaphira.transaction.mapper;

import com.zaphira.transaction.dto.TransactionRefundRequest;
import com.zaphira.transaction.dto.TransactionRefundResponse;
import com.zaphira.transaction.model.TransactionRefund;
import org.mapstruct.*;

import java.util.List;

/**
 * MapStruct mapper for TransactionRefund entity
 * 
 * @author Zaphira Platform
 * @version 2.0
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TransactionRefundMapper {
    
    /**
     * Map TransactionRefund entity to Response DTO
     */
    @Mapping(source = "transaction.id", target = "transactionId")
    @Mapping(source = "transaction.reference", target = "transactionReference")
    @Mapping(source = "transaction.amount", target = "originalTransactionAmount")
    TransactionRefundResponse toResponse(TransactionRefund refund);
    
    /**
     * Map list of TransactionRefund entities to Response DTOs
     */
    List<TransactionRefundResponse> toResponseList(List<TransactionRefund> refunds);
    
    /**
     * Map Request DTO to TransactionRefund entity
     * Note: transaction, status, and timestamps are set by service
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "refundReference", ignore = true)
    @Mapping(target = "transaction", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "processedAt", ignore = true)
    @Mapping(target = "approvedAt", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    TransactionRefund toEntity(TransactionRefundRequest request);
}
