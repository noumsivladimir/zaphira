package com.zaphira.wallet.repository;

import com.zaphira.common.model.entities.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUserId(Long userId);
    Optional<Wallet> findByWalletNumber(String walletNumber);
    boolean existsByWalletNumber(String walletNumber);
}

