package com.zaphira.service_user.services;

public interface SmsService {
    void sendSms(String phoneNumber, String message);
}