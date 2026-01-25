package com.taskmanager.filter;

import com.taskmanager.security.jwt.JWTFilter;
import com.taskmanager.service.JWTService;
import com.taskmanager.service.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JWTFilterTest {

    @Mock
    private JWTService jwtService;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private JWTFilter jwtFilter;

    @Test
    void authenticatesUserWhenValidTokenIsProvided() throws ServletException, IOException {
        String token = "valid.jwt.token";
        String email = "user@example.com";
        UserDetails userDetails = User.builder()
                .username(email)
                .password("password")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .build();

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractEmail(token)).thenReturn(email);
        when(jwtService.extractRole(token)).thenReturn("USER");
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(jwtService.validateToken(email, email, token)).thenReturn(true);

        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(null);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(securityContext).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void continuesFilterChainWhenNoAuthorizationHeaderPresent() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(jwtService, never()).extractEmail(anyString());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void continuesFilterChainWhenAuthorizationHeaderDoesNotStartWithBearer() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Basic sometoken");

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(jwtService, never()).extractEmail(anyString());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void continuesFilterChainWhenEmailCannotBeExtractedFromToken() throws ServletException, IOException {
        String token = "invalid.token";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractEmail(token)).thenReturn(null);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doesNotAuthenticateWhenUserIsAlreadyAuthenticated() throws ServletException, IOException {
        String token = "valid.jwt.token";
        String email = "user@example.com";
        Authentication existingAuth = mock(Authentication.class);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractEmail(token)).thenReturn(email);

        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(existingAuth);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doesNotAuthenticateWhenTokenValidationFails() throws ServletException, IOException {
        String token = "invalid.jwt.token";
        String email = "user@example.com";
        UserDetails userDetails = User.builder()
                .username(email)
                .password("password")
                .authorities(Collections.emptyList())
                .build();

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractEmail(token)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(jwtService.validateToken(email, email, token)).thenReturn(false);

        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(null);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void handlesEmptyBearerToken() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer ");

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(jwtService).extractEmail("");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void extractsTokenCorrectlyFromAuthorizationHeader() throws ServletException, IOException {
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U";
        String email = "test@example.com";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractEmail(token)).thenReturn(email);
        when(jwtService.extractRole(token)).thenReturn("USER");

        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(null);

        UserDetails userDetails = User.builder()
                .username(email)
                .password("password")
                .authorities(Collections.emptyList())
                .build();

        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(jwtService.validateToken(email, email, token)).thenReturn(true);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(jwtService).extractEmail(token);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void setsAuthenticationWithCorrectAuthorities() throws ServletException, IOException {
        String token = "valid.jwt.token";
        String email = "admin@example.com";
        UserDetails userDetails = User.builder()
                .username(email)
                .password("password")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .build();

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractEmail(token)).thenReturn(email);
        when(jwtService.extractRole(token)).thenReturn("ADMIN");
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(jwtService.validateToken(email, email, token)).thenReturn(true);

        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(null);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(securityContext).setAuthentication(argThat(auth ->
            auth.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"))
        ));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void handlesWhitespaceAroundBearerKeyword() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer");

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(jwtService, never()).extractEmail(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void continuesFilterChainWhenUserDetailsServiceThrowsException() {
        String token = "valid.jwt.token";
        String email = "user@example.com";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractEmail(token)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenThrow(new RuntimeException("User not found"));

        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(null);

        assertThrows(RuntimeException.class, () -> jwtFilter.doFilterInternal(request, response, filterChain));
    }

    @Test
    void handlesCaseInsensitiveBearerPrefix() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("bearer token123");

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(jwtService, never()).extractEmail(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void extractsEmailAndValidatesTokenWithDifferentEmailFormats() throws ServletException, IOException {
        String token = "valid.jwt.token";
        String email = "user+test@example.co.uk";
        UserDetails userDetails = User.builder()
                .username(email)
                .password("password")
                .authorities(Collections.emptyList())
                .build();

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractEmail(token)).thenReturn(email);
        when(jwtService.extractRole(token)).thenReturn("USER");
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(jwtService.validateToken(email, email, token)).thenReturn(true);

        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(null);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(userDetailsService).loadUserByUsername(email);
        verify(jwtService).validateToken(email, email, token);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void alwaysCallsFilterChainDoFilterRegardlessOfAuthenticationOutcome() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void setsAuthenticationDetailsFromRequest() throws ServletException, IOException {
        String token = "valid.jwt.token";
        String email = "user@example.com";
        UserDetails userDetails = User.builder()
                .username(email)
                .password("password")
                .authorities(Collections.emptyList())
                .build();

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.extractEmail(token)).thenReturn(email);
        when(jwtService.extractRole(token)).thenReturn("USER");
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(jwtService.validateToken(email, email, token)).thenReturn(true);

        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(null);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(securityContext).setAuthentication(argThat(auth -> auth.getDetails() != null));
        verify(filterChain).doFilter(request, response);
    }
}

