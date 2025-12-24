package com.zaphira.wallet.repository;

import com.zaphira.wallet.models.entities.WalletSubWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WalletSubWalletRepository extends JpaRepository<WalletSubWallet, Long> {

    List<WalletSubWallet> findByWalletId(Long walletId);

    List<WalletSubWallet> findWalletSubWalletBySubwalletId(Long subwalletId);

    List<WalletSubWallet> findBySubwalletId(Long subwalletId);

    @Query(value = "SELECT wallet_subwallet.wallet_id from wallet_subwallet where wallet_subwallet.subwallet_id = :subwalletId", nativeQuery = true)
    List<Long> findAllWalletIdBySuWalletNumber(@Param("subwalletId") Long subwalletId);

    void deleteByWalletIdAndSubwalletId(Long walletId, Long subwalletId);
}