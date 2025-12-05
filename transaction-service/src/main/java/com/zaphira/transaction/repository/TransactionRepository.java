package com.zaphira.transaction.repository;

import com.zaphira.transaction.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

	java.util.List<Transaction> findBySenderWalletId(Long senderWalletId);

	java.util.List<Transaction> findByReceiverWalletId(Long receiverWalletId);
}


