package com.zaphira.service_user.services;

public interface TelegramService {
    void sendMessage(String chatId, String message);
    void sendOtpMessage(String chatId, String otpCode);
}