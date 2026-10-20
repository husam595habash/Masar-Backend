package com.masar.backend.infra;

import com.masar.backend.exception.StorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SupabaseStorageServiceTest {

    private static final String BASE = "https://proj.supabase.co/storage/v1";

    private MockRestServiceServer server;
    private SupabaseStorageService storage;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        storage = new SupabaseStorageService("https://proj.supabase.co", "sb_secret_test", "cvs", builder);
    }

    @Test
    @DisplayName("upload posts the bytes to the bucket path with the secret key")
    void upload() {
        server.expect(requestTo(BASE + "/object/cvs/u1/a.pdf"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("apikey", "sb_secret_test"))
                .andExpect(header("x-upsert", "false"))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andRespond(withSuccess("{\"Key\":\"cvs/u1/a.pdf\"}", MediaType.APPLICATION_JSON));

        storage.upload("u1/a.pdf", "%PDF-1.7".getBytes(), "application/pdf");

        server.verify();
    }

    @Test
    @DisplayName("a provider error becomes StorageException")
    void uploadFails() {
        server.expect(requestTo(BASE + "/object/cvs/u1/a.pdf"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> storage.upload("u1/a.pdf", new byte[]{1}, "application/pdf"))
                .isInstanceOf(StorageException.class);
    }

    @Test
    @DisplayName("signed url asks for the ttl in seconds and returns a full link")
    void signedUrl() {
        server.expect(requestTo(BASE + "/object/sign/cvs/u1/a.pdf"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"expiresIn\":300}"))
                .andRespond(withSuccess("{\"signedURL\":\"/object/sign/cvs/u1/a.pdf?token=abc\"}",
                        MediaType.APPLICATION_JSON));

        String url = storage.createSignedUrl("u1/a.pdf", Duration.ofMinutes(5));

        assertThat(url).isEqualTo(BASE + "/object/sign/cvs/u1/a.pdf?token=abc");
    }

    @Test
    @DisplayName("delete calls the object endpoint")
    void delete() {
        server.expect(requestTo(BASE + "/object/cvs/u1/a.pdf"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withSuccess());

        storage.delete("u1/a.pdf");

        server.verify();
    }

    @Test
    @DisplayName("paths with .. or odd characters are refused before any request")
    void unsafePath() {
        assertThatThrownBy(() -> storage.delete("../other/a.pdf")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage.upload("a b.pdf", new byte[]{1}, "application/pdf"))
                .isInstanceOf(IllegalArgumentException.class);
        server.verify();
    }
}
