package com.zaphira.notification.controller;

import com.zaphira.notification.service.TelegramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationTestController {

    private final TelegramService telegramService;

    @PostMapping("/test/telegram")
    public ResponseEntity<?> testTelegram(@RequestParam String chatId, @RequestParam String message) {
        try {
            telegramService.sendMessage(chatId, message);
            return ResponseEntity.ok().body("{\"status\":\"sent\", \"chatId\":\"" + chatId + "\"}");
        } catch (Exception e) {
            log.error("Test failed", e);
            return ResponseEntity.status(500).body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    @PostMapping("/test/welcome")
    public ResponseEntity<?> testWelcome(@RequestParam String chatId) {
        try {
            telegramService.sendUserWelcomeMessage(chatId, "Test", "User");
            return ResponseEntity.ok().body("{\"status\":\"welcome_sent\"}");
        } catch (Exception e) {
            log.error("Welcome test failed", e);
            return ResponseEntity.status(500).body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    @PostMapping("/test/transaction")
    public ResponseEntity<?> testTransaction(@RequestParam String chatId) {
        try {
            telegramService.sendTransactionMessage(chatId, "TXN-123456", "50000", "CREDIT", "COMPLETED");
            return ResponseEntity.ok().body("{\"status\":\"transaction_sent\"}");
        } catch (Exception e) {
            log.error("Transaction test failed", e);
            return ResponseEntity.status(500).body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    @PostMapping("/test/wallet")
    public ResponseEntity<?> testWallet(@RequestParam String chatId) {
        try {
            telegramService.sendWalletCreatedMessage(chatId, "WALLET-123456");
            telegramService.sendBalanceUpdateMessage(chatId, "WALLET-123456", "150000 XAF");
            return ResponseEntity.ok().body("{\"status\":\"wallet_notifications_sent\"}");
        } catch (Exception e) {
            log.error("Wallet test failed", e);
            return ResponseEntity.status(500).body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}