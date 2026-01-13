package com.zaphira.wallet.dto.request;


import lombok.Data;

@Data
public class CreateMerchantWalletRequest {
    private String merchantName;
    private String walletNumber;

}
