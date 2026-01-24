package com.zaphira.service_user.services;

public interface EmailService {
    void sendEmail(String to, String subject, String body);
    void sendHtmlEmail(String to, String subject, String htmlBody);
    
    // KYC-related email methods
    void sendKYCSubmissionConfirmation(String to, String firstName);
    void sendKYCResubmissionConfirmation(String to, String firstName);
    void sendKYCApprovalEmail(String to, String firstName);
    void sendKYCRejectionEmail(String to, String firstName, String reason, boolean canResubmit);
    void sendKYCExpiryNotification(String to, String firstName);
}