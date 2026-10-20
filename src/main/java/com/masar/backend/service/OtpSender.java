package com.masar.backend.service;

/**
 * Delivers a verification code to a user.
 * Today it is done by email; another channel (SMS, another provider) can implement this too.
 */
public interface OtpSender {

    /**
     * Sends the code to the user.
     * Throws EmailSendException if it could not be delivered.
     */
    void sendOtp(String to, String otp);
}
