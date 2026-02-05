package com.zaphira.wallet.mapper;

import com.zaphira.wallet.dto.WalletDTO;
import com.zaphira.wallet.model.entities.Wallet;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;


@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface WalletMapper {

    @Mapping(target = "currency", ignore = true)
    WalletDTO toDTO(Wallet wallet);

    @Mapping(target = "closedAt", ignore = true)
    @Mapping(target = "frozenBy", ignore = true)
    @Mapping(target = "lastLimitReset", ignore = true)
    @Mapping(target = "merchantCode", ignore = true)
    @Mapping(target = "merchantName", ignore = true)
    @Mapping(target = "permissions", ignore = true)
    @Mapping(target = "statusHistory", ignore = true)
    @Mapping(target = "version", ignore = true)
    Wallet toEntity(WalletDTO walletDTO);
}