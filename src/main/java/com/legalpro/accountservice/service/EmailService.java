package com.legalpro.accountservice.service;

public interface EmailService {
    void sendEmail(String to, String subject, String body);
}
