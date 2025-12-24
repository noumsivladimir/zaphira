package com.zaphira.transaction.integration.wallet;

import com.zaphira.common.dto.WalletDTO;
//import com.zaphira.transaction.integration.wallet.dto.WalletDetailsResponse;
//import com.zaphira.transaction.integration.wallet.WalletTransferRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;

@FeignClient(name = "wallet-service")
public interface FeignWalletClient {

    @GetMapping("/api/wallets/{walletNumber}")
    WalletDTO getWalletByNumber(@PathVariable String walletNumber);

    @GetMapping("/api/wallets/user/{userId}")
    WalletDTO getWalletByUserId(@PathVariable Long userId);

    @PostMapping("/api/wallets/transfer")
    void executeTransfer(@RequestBody WalletTransferRequest request);

    @PostMapping("/api/wallets/{walletNumber}/debit")
    void debit(@PathVariable String walletNumber, @RequestBody BigDecimal amount);

    @PostMapping("/api/wallets/{walletNumber}/credit")
    void credit(@PathVariable String walletNumber, @RequestBody BigDecimal amount);

    @PostMapping("/api/wallets/{walletNumber}/block-funds")
    void blockFunds(@PathVariable String walletNumber, @RequestBody BigDecimal amount);

    @PostMapping("/api/wallets/{walletNumber}/unblock-funds")
    void unblockFunds(@PathVariable String walletNumber, @RequestBody BigDecimal amount);
}

