package com.masar.backend.infra;

import com.masar.backend.exception.StorageException;
import com.masar.backend.service.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Supabase Storage through its REST API. It uses the secret key, so it must only run on
 * the server. The bucket is private: files are read through short-lived signed links.
 */
@Service
public class SupabaseStorageService implements StorageService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SupabaseStorageService.class);

    /** Letters, digits, dot, dash, underscore and slash only. Blocks "..", spaces and odd characters. */
    private static final Pattern SAFE_PATH = Pattern.compile("^[A-Za-z0-9_-][A-Za-z0-9._/-]*$");

    private final RestClient client;
    private final String storageUrl;
    private final String bucket;


    public SupabaseStorageService(@Value("${supabase.url}") String url,
                                  @Value("${supabase.secret-key}") String secretKey,
                                  @Value("${supabase.bucket}") String bucket,
                                  RestClient.Builder builder) {
        this.storageUrl = url.replaceAll("/+$", "") + "/storage/v1";   // url is https://<project>.supabase.co
        this.bucket = bucket;
        this.client = builder
                .baseUrl(storageUrl)
                .defaultHeader("apikey", secretKey)
                .build();
    }

    @Override
    public void upload(String path, byte[] content, String contentType) {
        checkPath(path);
        try {
            client.post()
                    .uri("/object/" + bucket + "/" + path)
                    .contentType(MediaType.parseMediaType(contentType))
                    .header("x-upsert", "false")   // never silently replace an existing file
                    .body(content)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("Supabase upload failed for {}", path, e);
            throw new StorageException();
        }
    }

    @Override
    public String createSignedUrl(String path, Duration ttl) {
        checkPath(path);
        try {
            Map<?, ?> response = client.post()
                    .uri("/object/sign/" + bucket + "/" + path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("expiresIn", ttl.toSeconds()))
                    .retrieve()
                    .body(Map.class);

            Object signed = response == null ? null : response.get("signedURL");
            if (signed == null) {
                throw new StorageException();
            }
            // Supabase answers with a path that starts with /object/sign/..., add the base back
            return storageUrl + signed;
        } catch (RestClientException e) {
            log.error("Supabase signed URL failed for {}", path, e);
            throw new StorageException();
        }
    }

    @Override
    public void delete(String path) {
        checkPath(path);
        try {
            client.delete()
                    .uri("/object/" + bucket + "/" + path)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("Supabase delete failed for {}", path, e);
            throw new StorageException();
        }
    }

    private static void checkPath(String path) {
        if (path == null || !SAFE_PATH.matcher(path).matches() || path.contains("..")) {
            throw new IllegalArgumentException("Unsafe storage path");
        }
    }

}
