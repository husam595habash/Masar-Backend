package com.masar.backend.exception;

public class EmailSendException extends RuntimeException {

    public EmailSendException() {
        super("Could not send the verification email, please request a new code");
    }
}
