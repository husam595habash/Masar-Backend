package com.masar.backend.service;

import java.time.Duration;

/**
 * Stores files outside the database. The rest of the app depends on this interface,
 * so the provider (Supabase today) can change without touching the services.
 * Paths look like "cvs/{userId}/{uuid}.pdf" and are the only thing saved in the database.
 */
public interface StorageService {

    /** Saves the file under this path. Throws StorageException if the provider fails. */
    void upload(String path, byte[] content, String contentType);

    /** A temporary link to read a private file. It stops working after the ttl. */
    String createSignedUrl(String path, Duration ttl);

    /** Deletes the file. Throws StorageException if the provider fails. */
    void delete(String path);
}
