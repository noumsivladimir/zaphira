package com.zaphira.notification.service;

public interface SmsService {

	/**
	 * Envoie un SMS contenant un OTP au numéro donné.
	 * @param phoneNumber numéro destinataire en format E.164
	 * @param code code OTP (ex: "123456")
	 */
	void sendOtpSms(String phoneNumber, String code);

	/**
	 * Envoie un message SMS générique.
	 */
	void sendSms(String phoneNumber, String message);

}
