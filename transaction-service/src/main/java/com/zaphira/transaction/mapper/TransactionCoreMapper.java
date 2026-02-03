package com.zaphira.transaction.mapper;

import com.zaphira.transaction.dto.core.TransactionCoreDTO;
import com.zaphira.transaction.model.core.TransactionCore;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * TransactionCoreMapper - MapStruct mapper for LOT 1 + LOT 2
 * 
 * Maps TransactionCore entity to DTO including fees and metadata
 */
@Mapper(componentModel = "spring")
public interface TransactionCoreMapper {

    /**
     * Convert entity to DTO
     */
    @Mapping(target = "currency", expression = "java(transactionCore.getCurrency() != null ? transactionCore.getCurrency().name() : null)")
    @Mapping(target = "feeAmount", source = "feeAmount")
    @Mapping(target = "totalAmount", source = "totalAmount")
    @Mapping(target = "platformFee", expression = "java(transactionCore.getFees() != null ? transactionCore.getFees().getPlatformFee() : null)")
    @Mapping(target = "merchantFee", expression = "java(transactionCore.getFees() != null ? transactionCore.getFees().getMerchantFee() : null)")
    @Mapping(target = "failureReason", expression = "java(transactionCore.getMetadata() != null ? transactionCore.getMetadata().getFailureReason() : null)")
    @Mapping(target = "retryCount", expression = "java(transactionCore.getMetadata() != null ? transactionCore.getMetadata().getRetryCount() : null)")
    @Mapping(target = "maxRetryAttempts", expression = "java(transactionCore.getMetadata() != null ? transactionCore.getMetadata().getMaxRetryAttempts() : null)")
    TransactionCoreDTO toDTO(TransactionCore transactionCore);

    /**
     * Convert list of entities to DTOs
     */
    List<TransactionCoreDTO> toDTOList(List<TransactionCore> transactions);
}
