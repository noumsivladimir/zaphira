package com.zaphira.zaphira;


import com.zaphira.zaphira.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired WalletService walletService;

    @GetMapping("/{userId}")
    public Double getBalance(@PathVariable Long userId) {
        return walletService.getBalance(userId);
    }
}
