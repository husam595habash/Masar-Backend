package com.masar.backend.security;

import com.masar.backend.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    /**
     * Reads the Base64 secret and the expiration from application.properties,
     * and builds the HMAC signing key once at startup.
     */
    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationMs = expirationMs;
    }

    /**
     * Builds and signs a JWT: subject = email, claim role, issued-at, expiration.
     * Returns the compact string header.payload.signature.
     */
    public String generateToken(User user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getRole().name())
                .claim("ver", user.getAuthVersion())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    /**
     * Reads the email (subject) from a valid token.
     */
    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }


    public int extractAuthVersion(String token) {
        Integer ver = parseClaims(token).get("ver", Integer.class);
        return ver == null ? 0 : ver;
    }

    /**
     * Returns true if the signature is correct and the token is not expired,
     * false for any invalid or malformed token (never throws).
     */
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Verifies the signature and expiration, then returns the token payload (claims).
     * Throws JwtException if the token is invalid.
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
