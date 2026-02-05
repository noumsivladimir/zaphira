package com.zaphira.user.service;

public interface SmsService {
    void sendSms(String phoneNumber, String message);
}
