package com.zaphira.transaction.integration.wallet;

import com.zaphira.transaction.integration.wallet.dto.WalletDetailsResponse;

public interface WalletClient {

    WalletDetailsResponse getWalletDetails(String walletNumber);

    void executeTransfer(WalletTransferRequest request);
}


