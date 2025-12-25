package com.zaphira.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Service
public class TelegramService {

    private final WebClient webClient;

    public TelegramService(WebClient webClient) {
        this.webClient = webClient;
    }

    @Value("${telegram.bot.token:}")
    private String botToken;

    @Value("${telegram.bot.chat-id:}")
    private String defaultChatId;

    public void sendMessage(String chatId, String message) {
        if (botToken == null || botToken.isEmpty()) {
            log.warn("Telegram bot token not configured. Skipping message send.");
            return;
        }

        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";

        Map<String, Object> requestBody = Map.of(
            "chat_id", chatId != null ? chatId : defaultChatId,
            "text", message,
            "parse_mode", "HTML"
        );

        webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response -> log.info("Telegram message sent successfully to chat: {}", chatId))
                .doOnError(error -> log.error("Failed to send Telegram message to chat: {}", chatId, error))
                .onErrorResume(error -> Mono.empty())
                .block();
    }

    public void sendUserWelcomeMessage(String chatId, String firstName, String lastName) {
        String message = String.format(
            "🎉 <b>Bienvenue sur Zaphira!</b>\n\n" +
            "Bonjour <b>%s %s</b>!\n\n" +
            "Votre compte a été créé avec succès.\n" +
            "Vous pouvez maintenant utiliser tous nos services.\n\n" +
            "📱 <i>Profitez de votre expérience!</i>",
            firstName, lastName
        );
        sendMessage(chatId, message);
    }

    public void sendWalletCreatedMessage(String chatId, String walletNumber) {
        String message = String.format(
            "💳 <b>Portefeuille créé!</b>\n\n" +
            "Votre nouveau portefeuille a été créé:\n" +
            "<code>%s</code>\n\n" +
            "Vous pouvez maintenant effectuer des transactions.",
            walletNumber
        );
        sendMessage(chatId, message);
    }

    public void sendTransactionMessage(String chatId, String transactionRef, String amount, String type, String status) {
        String emoji = switch (type.toLowerCase()) {
            case "credit" -> "➕";
            case "debit" -> "➖";
            case "transfer" -> "↔️";
            default -> "💰";
        };

        String statusEmoji = switch (status.toLowerCase()) {
            case "completed", "success" -> "✅";
            case "pending" -> "⏳";
            case "failed", "error" -> "❌";
            default -> "ℹ️";
        };

        String message = String.format(
            "%s <b>Transaction %s</b>\n\n" +
            "Référence: <code>%s</code>\n" +
            "Montant: <b>%s</b>\n" +
            "Statut: %s <b>%s</b>",
            emoji, type, transactionRef, amount, statusEmoji, status
        );
        sendMessage(chatId, message);
    }

    public void sendBalanceUpdateMessage(String chatId, String walletNumber, String newBalance) {
        String message = String.format(
            "💰 <b>Mise à jour du solde</b>\n\n" +
            "Portefeuille: <code>%s</code>\n" +
            "Nouveau solde: <b>%s</b>",
            walletNumber, newBalance
        );
        sendMessage(chatId, message);
    }
}