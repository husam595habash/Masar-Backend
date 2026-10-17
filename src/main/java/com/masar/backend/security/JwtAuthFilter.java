package com.masar.backend.security;

import com.masar.backend.service.AuthVersionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final AuthVersionService authVersionService;


    public JwtAuthFilter(JwtService jwtService,
                         CustomUserDetailsService userDetailsService,
                         AuthVersionService authVersionService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.authVersionService = authVersionService;
    }

    /**
     * Runs once per request. Reads 'Authorization: Bearer <token>', validates it,
     * loads the user from the database and stores it in the SecurityContext.
     * No token or an invalid token: continues anonymously, and the security rules
     * decide the result (401 for protected endpoints).
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        // No token: continue anonymously. Protected endpoints will answer 401.
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length());

        if (jwtService.isTokenValid(token)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String email = jwtService.extractEmail(token);

                // The token is valid only if it carries the user's current authVersion.
                // A password change raises the version, so older tokens stop working.
                if (jwtService.extractAuthVersion(token) != authVersionService.current(email)) {
                    filterChain.doFilter(request, response);
                    return;
                }

                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (UsernameNotFoundException ignored) {
                // Token is valid but the user no longer exists: stay anonymous.
            }
        }

        filterChain.doFilter(request, response);
    }
}
