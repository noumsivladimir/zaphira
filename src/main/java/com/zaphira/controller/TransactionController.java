package com.zaphira.zaphira;


import com.zaphira.zaphira.Transaction;
import com.zaphira.zaphira.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transaction")
public class TransactionController {

    @Autowired WalletService walletService;

    @PostMapping("/send")
    public String send(@RequestParam Long senderId,
                       @RequestParam Long receiverId,
                       @RequestParam Double amount) {

        walletService.sendMoney(senderId, receiverId, amount);
        return "Transfer done";
    }

    @PostMapping("/withdraw")
    public String withdraw(@RequestParam Long userId,
                           @RequestParam Double amount) {

        walletService.withdraw(userId, amount);
        return "Withdrawal done";
    }

    @GetMapping("/history/{userId}")
    public List<Transaction> history(@PathVariable Long userId) {
        return walletService.getHistory(userId);
    }
}

