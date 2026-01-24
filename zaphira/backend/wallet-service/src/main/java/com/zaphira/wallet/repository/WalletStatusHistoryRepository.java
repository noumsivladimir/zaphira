package com.zaphira.wallet.repository;

import com.zaphira.wallet.models.entities.WalletStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WalletStatusHistoryRepository extends JpaRepository<WalletStatusHistory, Long> {

//    List<WalletStatusHistory> findByWalletIdOrderByChangedAtDesc(Long walletId);
//
//    List<WalletStatusHistory> findByWalletWalletNumberOrderByChangedAtDesc(String walletNumber);
}