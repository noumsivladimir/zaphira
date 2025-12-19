package com.zaphira.wallet.repository;

import com.zaphira.wallet.models.entities.SubWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SubWalletRepository extends JpaRepository <SubWallet, UUID> {
}
