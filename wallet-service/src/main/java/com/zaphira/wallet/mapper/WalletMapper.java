package com.zaphira.wallet.mapper;

import com.zaphira.wallet.dto.WalletDTO;
import com.zaphira.wallet.models.entities.Wallet;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;


@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface WalletMapper {

    WalletDTO toDTO(Wallet wallet);

    Wallet toEntity(WalletDTO walletDTO);
}