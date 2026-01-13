package com.zaphira.wallet.repository;

import com.zaphira.wallet.models.entities.Wallet;
import com.zaphira.wallet.models.enums.WalletStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {



    Optional<Wallet> findByWalletNumber(String walletNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.walletNumber = :walletNumber")
    Optional<Wallet> findByWalletNumberWithLock(@Param("walletNumber") String walletNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.id = :id")
    Optional <Wallet> findByWalletIdWithLock(@Param("id") Long walletId);

//    @Lock(LockModeType.PESSIMISTIC_WRITE)
//    @Query("SELECT w FROM Wallet w WHERE w.id = :id")
//    Wallet loadWalletForUpdateByIdWithLock(@Param("id") Long id);



    List<Wallet> findByUserIdAndStatus(Long userId, WalletStatus status);

    List<Wallet> findAllById(Iterable<Long> ids);

    Optional<Wallet> findByUserIdAndIsPrimaryTrue(Long userId);

    boolean existsByWalletNumber(String walletNumber);

//    @Query("SELECT w FROM Wallet w WHERE w.userId = :userId AND w.type = :type AND w.status != 'CLOSED'")
//    Optional<Wallet> findActiveWalletByUserIdAndType(@Param("userId") Long userId, @Param("type") WalletType type);

//    @Query("SELECT COUNT(w) FROM Wallet w WHERE w.userId = :userId AND w.status != 'CLOSED'")
//    long countActiveWalletsByUserId(@Param("userId") Long userId);
//
    @Query("SELECT w FROM Wallet w WHERE w.status = :status")
    List<Wallet> findAllByStatus(@Param("status") WalletStatus status);
}

