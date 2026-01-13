package com.zaphira.wallet.service;


import com.zaphira.wallet.exception.WalletNotFoundException;
import com.zaphira.wallet.models.entities.Wallet;
import com.zaphira.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@Slf4j
@RequiredArgsConstructor
public class WalletQueryService {

    private final WalletRepository walletRepository;


    public Wallet findWalletByNumber(String walletNumber) {
        return walletRepository.findByWalletNumber(walletNumber)
                .orElseThrow(() -> new WalletNotFoundException("Wallet non trouvé: " + walletNumber));
    }

    public List<Wallet> findAllById(Iterable<Long> ids){
        return walletRepository.findAllById(ids);
    }

    public Wallet findWalletByNumberWithLock(String walletNumber) {
        return walletRepository.findByWalletNumberWithLock(walletNumber)
                .orElseThrow(() -> new WalletNotFoundException("Wallet non trouvé: " + walletNumber));
    }




}
