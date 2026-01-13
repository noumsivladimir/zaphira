package com.zaphira.wallet.repository;

import com.zaphira.wallet.models.entities.SubWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubWalletRepository extends JpaRepository <SubWallet, Long> {


    //  Query pour charger les relations
    @Query("SELECT sw FROM SubWallet sw LEFT JOIN FETCH sw.managingWallets WHERE sw.id = :id")
    Optional<SubWallet> findByIdWithWallets(@Param("id") Long id);

    @Query("SELECT DISTINCT sw FROM SubWallet sw LEFT JOIN FETCH sw.managingWallets")
    List<SubWallet> findAllWithWallets();

//    @Query("SELECT sw.walletId FROM SubWallet sw WHERE sw.id = :id")

}
