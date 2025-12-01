package com.zaphira.wallet.client;

import com.zaphira.common.dto.TransactionDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "transaction-service")
public interface TransactionServiceClient {
    @PostMapping("/api/transactions")
    TransactionDTO createTransaction(@RequestBody Object transactionRequest);
}

