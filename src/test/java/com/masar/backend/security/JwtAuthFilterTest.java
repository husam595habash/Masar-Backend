package com.masar.backend.security;

import com.masar.backend.service.AuthVersionService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock private JwtService jwtService;
    @Mock private CustomUserDetailsService userDetailsService;
    @Mock private AuthVersionService authVersionService;

    @InjectMocks
    private JwtAuthFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain chain;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        userDetails = new org.springframework.security.core.userdetails.User(
                "ali@mail.com", "ENCODED", List.of(new SimpleGrantedAuthority("ROLE_JOB_SEEKER")));
    }

    // The SecurityContext is shared by the whole thread, so always leave it clean.
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // The filter must always pass the request on, whatever the result of the checks
    private void assertRequestContinued() {
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("no Authorization header: continues as anonymous")
    void noHeader() throws ServletException, IOException {
        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertRequestContinued();
        verifyNoInteractions(jwtService, userDetailsService, authVersionService);
    }

    @Test
    @DisplayName("invalid token: continues as anonymous")
    void invalidToken() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer bad.token");
        when(jwtService.isTokenValid("bad.token")).thenReturn(false);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertRequestContinued();
        verifyNoInteractions(userDetailsService, authVersionService);
    }

    @Test
    @DisplayName("valid token with the current authVersion: the user is authenticated")
    void validToken_currentVersion() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer good.token");
        when(jwtService.isTokenValid("good.token")).thenReturn(true);
        when(jwtService.extractEmail("good.token")).thenReturn("ali@mail.com");
        when(jwtService.extractAuthVersion("good.token")).thenReturn(3);
        when(authVersionService.current("ali@mail.com")).thenReturn(3);
        when(userDetailsService.loadUserByUsername("ali@mail.com")).thenReturn(userDetails);

        filter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("ali@mail.com");
        assertThat(auth.getAuthorities()).extracting("authority").containsExactly("ROLE_JOB_SEEKER");
        assertRequestContinued();
    }

    @Test
    @DisplayName("valid token but old authVersion (password changed): continues as anonymous")
    void validToken_oldVersion() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer old.token");
        when(jwtService.isTokenValid("old.token")).thenReturn(true);
        when(jwtService.extractEmail("old.token")).thenReturn("ali@mail.com");
        when(jwtService.extractAuthVersion("old.token")).thenReturn(3);     // token was issued with 3
        when(authVersionService.current("ali@mail.com")).thenReturn(4);     // user is now at 4

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(userDetailsService, never()).loadUserByUsername("ali@mail.com");
        assertRequestContinued();
    }

    @Test
    @DisplayName("valid token but the user no longer exists: continues as anonymous")
    void validToken_userDeleted() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer good.token");
        when(jwtService.isTokenValid("good.token")).thenReturn(true);
        when(jwtService.extractEmail("good.token")).thenReturn("ali@mail.com");
        when(jwtService.extractAuthVersion("good.token")).thenReturn(0);
        when(authVersionService.current("ali@mail.com")).thenReturn(0);
        when(userDetailsService.loadUserByUsername("ali@mail.com"))
                .thenThrow(new UsernameNotFoundException("User not found"));

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertRequestContinued();
    }
}
