package com.zaphira.user.service;

public interface TelegramService {
    void sendMessage(String chatId, String message);
    void sendOtpMessage(String chatId, String otpCode);
}
