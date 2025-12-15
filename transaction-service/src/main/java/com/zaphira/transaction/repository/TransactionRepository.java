package com.zaphira.transaction.repository;

import com.zaphira.transaction.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.zaphira.common.model.entities.Wallet;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

	java.util.List<Transaction> findBySenderWallet(Wallet senderWallet);

	java.util.List<Transaction> findByReceiverWallet(Wallet receiverWallet);
}


