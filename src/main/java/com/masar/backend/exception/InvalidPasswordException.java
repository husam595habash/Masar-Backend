package com.masar.backend.exception;

/**
 * Thrown when the user gives a wrong current password, or a new password that is
 * the same as the old one.
 */
public class InvalidPasswordException extends RuntimeException {

    public InvalidPasswordException(String message) {
        super(message);
    }
}
