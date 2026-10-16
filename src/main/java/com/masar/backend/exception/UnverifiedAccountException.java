package com.masar.backend.exception;

public class UnverifiedAccountException extends RuntimeException {

    public UnverifiedAccountException() {
        super("Please verify your email first");
    }
}
