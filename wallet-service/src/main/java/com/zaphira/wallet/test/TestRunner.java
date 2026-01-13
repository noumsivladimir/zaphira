package com.zaphira.wallet.test;

import com.zaphira.wallet.models.entities.Wallet;
import com.zaphira.wallet.repository.WalletRepository;
import com.zaphira.wallet.repository.WalletSubWalletRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TestRunner implements CommandLineRunner {

    private final WalletSubWalletRepository walletSubWalletRepository;
    private final WalletRepository walletRepository;

    public TestRunner(WalletSubWalletRepository walletSubWalletRepository, WalletRepository walletRepository) {
        this.walletSubWalletRepository = walletSubWalletRepository;
        this.walletRepository = walletRepository;
    }


    @Override
    public void run(String... args) throws Exception {


        System.out.println("=== TEST START ===");

        List < Long> walletNumber = walletSubWalletRepository.findAllWalletIdBySuWalletNumber(606L);

        System.out.println(walletNumber);

        List < Wallet> wallets = walletRepository.findAllById(walletNumber);
        System.out.println(wallets);

        System.out.println("=== TEST END ===");
    }
}
