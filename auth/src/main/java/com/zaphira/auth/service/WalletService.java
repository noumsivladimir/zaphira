package com.zaphira.auth.service;

import com.zaphira.auth.model.ActivityLog;
import com.zaphira.auth.model.Transaction;
import com.zaphira.auth.model.TransactionType;
import com.zaphira.auth.model.Wallet;
import com.zaphira.auth.repository.ActivityLogRepository;
import com.zaphira.auth.repository.TransactionRepository;
import com.zaphira.auth.repository.UserRepository;
import com.zaphira.auth.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WalletService {

    @Autowired WalletRepository walletRepo;
    @Autowired UserRepository userRepo;
    @Autowired TransactionRepository transactionRepo;
    @Autowired ActivityLogRepository logRepo;

    public void sendMoney(Long senderId, Long receiverId, Double amount) {
        Wallet senderWallet = walletRepo.findByUserId(senderId);
        Wallet receiverWallet = walletRepo.findByUserId(receiverId);

        if (senderWallet.getBalance() < amount)
            throw new RuntimeException("Solde insuffisant");

        senderWallet.setBalance(senderWallet.getBalance() - amount);
        receiverWallet.setBalance(receiverWallet.getBalance() + amount);

        walletRepo.save(senderWallet);
        walletRepo.save(receiverWallet);

        saveTransaction(senderId, amount, "Sending money", TransactionType.SEND);
        saveTransaction(receiverId, amount, "Money received", TransactionType.RECEIVE);
        logAction(senderId, "Transfer money");
    }

    public void withdraw(Long userId, Double amount) {
        Wallet wallet = walletRepo.findByUserId(userId);

        if (wallet.getBalance() < amount)
            throw new RuntimeException("Solde insuffisant");

        wallet.setBalance(wallet.getBalance() - amount);
        walletRepo.save(wallet);

        saveTransaction(userId, amount, "Withdrawal", TransactionType.WITHDRAW);
        logAction(userId, "Withdraw money");
    }

    public Double getBalance(Long userId) {
        return walletRepo.findByUserId(userId).getBalance();
    }

    public List<Transaction> getHistory(Long userId) {
        return transactionRepo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    private void saveTransaction(Long userId, Double amount, String description, TransactionType type) {
        Transaction tx = new Transaction();
        tx.setAmount(amount);
        tx.setDescription(description);
        tx.setType(type);
        tx.setUser(userRepo.findById(userId).orElseThrow());
        transactionRepo.save(tx);
    }

    private void logAction(Long userId, String action) {
        ActivityLog log = new ActivityLog();
        log.setUser(userRepo.findById(userId).orElseThrow());
        log.setAction(action);
        logRepo.save(log);
    }
}
