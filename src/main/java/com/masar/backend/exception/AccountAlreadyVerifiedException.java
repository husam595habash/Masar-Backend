package com.masar.backend.exception;

public class AccountAlreadyVerifiedException extends RuntimeException {

    public AccountAlreadyVerifiedException() {
        super("Account is already verified");
    }
}
