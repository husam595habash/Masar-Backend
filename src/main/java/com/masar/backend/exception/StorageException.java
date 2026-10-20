package com.masar.backend.exception;

public class StorageException extends RuntimeException {

    public StorageException() {
        super("Could not reach the file storage, please try again");
    }
}
