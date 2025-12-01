package com.zaphira.transaction.integration.wallet;

import com.zaphira.common.dto.WalletDTO;
import com.zaphira.transaction.integration.wallet.dto.WalletDetailsResponse;
import com.zaphira.transaction.integration.wallet.WalletTransferRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "wallet-service")
public interface FeignWalletClient {

    @GetMapping("/api/wallets/{walletNumber}")
    WalletDTO getWalletByNumber(@PathVariable String walletNumber);

    @PostMapping("/api/wallets/transfer")
    void executeTransfer(@RequestBody WalletTransferRequest request);
}

