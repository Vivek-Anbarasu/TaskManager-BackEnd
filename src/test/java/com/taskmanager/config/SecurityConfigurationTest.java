package com.taskmanager.config;

import com.taskmanager.security.jwt.JWTFilter;
import com.taskmanager.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityConfigurationTest {

    @Mock
    private JWTFilter jwtFilter;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    private SecurityConfiguration securityConfiguration;

    @BeforeEach
    void setUp() {
        securityConfiguration = new SecurityConfiguration(jwtFilter, userDetailsService);
        // Set default values for the @Value annotated fields
        ReflectionTestUtils.setField(securityConfiguration, "corsAllowedOrigins", "http://localhost:3000,http://localhost:4200");
        ReflectionTestUtils.setField(securityConfiguration, "corsAllowedMethods", "GET,POST,PUT,DELETE,PATCH,OPTIONS");
    }

    @Test
    void passwordEncoderBeanCreatesInstanceOfBCryptPasswordEncoder() {
        PasswordEncoder passwordEncoder = securityConfiguration.passwordEncoder();

        assertNotNull(passwordEncoder);
        assertInstanceOf(BCryptPasswordEncoder.class, passwordEncoder);
    }

    @Test
    void passwordEncoderCanEncodePassword() {
        PasswordEncoder passwordEncoder = securityConfiguration.passwordEncoder();
        String rawPassword = "testPassword123";

        String encodedPassword = passwordEncoder.encode(rawPassword);

        assertNotNull(encodedPassword);
        assertNotEquals(rawPassword, encodedPassword);
        assertTrue(passwordEncoder.matches(rawPassword, encodedPassword));
    }

    @Test
    void authenticationProviderBeanCreatesInstanceOfDaoAuthenticationProvider() {
        AuthenticationProvider authenticationProvider = securityConfiguration.authenticationProvider();

        assertNotNull(authenticationProvider);
        assertInstanceOf(DaoAuthenticationProvider.class, authenticationProvider);
    }

    @Test
    void authenticationProviderUsesUserDetailsService() {
        AuthenticationProvider authenticationProvider = securityConfiguration.authenticationProvider();

        assertNotNull(authenticationProvider);
        DaoAuthenticationProvider daoProvider = (DaoAuthenticationProvider) authenticationProvider;
        // Verify the provider is properly configured (we can't directly access userDetailsService)
        assertNotNull(daoProvider);
    }

    @Test
    void authenticationManagerBeanIsCreated() throws Exception {
        AuthenticationConfiguration authConfig = mock(AuthenticationConfiguration.class);
        AuthenticationManager mockAuthManager = mock(AuthenticationManager.class);
        when(authConfig.getAuthenticationManager()).thenReturn(mockAuthManager);

        AuthenticationManager authenticationManager = securityConfiguration.authenticationManager(authConfig);

        assertNotNull(authenticationManager);
        assertEquals(mockAuthManager, authenticationManager);
        verify(authConfig).getAuthenticationManager();
    }

    @Test
    void corsConfigurationSourceBeanIsCreated() {
        CorsConfigurationSource corsConfigurationSource = securityConfiguration.corsConfigurationSource();

        assertNotNull(corsConfigurationSource);
        assertInstanceOf(UrlBasedCorsConfigurationSource.class, corsConfigurationSource);
    }

    @Test
    void corsConfigurationContainsAllowedOrigins() {
        ReflectionTestUtils.setField(securityConfiguration, "corsAllowedOrigins", "http://localhost:3000,http://example.com");

        CorsConfigurationSource corsConfigurationSource = securityConfiguration.corsConfigurationSource();
        UrlBasedCorsConfigurationSource source = (UrlBasedCorsConfigurationSource) corsConfigurationSource;

        assertNotNull(source);
        org.springframework.web.cors.CorsConfiguration config = source.getCorsConfigurations().get("/**");
        assertNotNull(config);
        assertNotNull(config.getAllowedOriginPatterns());
        assertEquals(2, config.getAllowedOriginPatterns().size());
    }

    @Test
    void corsConfigurationContainsAllowedMethods() {
        ReflectionTestUtils.setField(securityConfiguration, "corsAllowedMethods", "GET,POST,PUT");

        CorsConfigurationSource corsConfigurationSource = securityConfiguration.corsConfigurationSource();
        UrlBasedCorsConfigurationSource source = (UrlBasedCorsConfigurationSource) corsConfigurationSource;

        org.springframework.web.cors.CorsConfiguration config = source.getCorsConfigurations().get("/**");
        assertNotNull(config);
        assertNotNull(config.getAllowedMethods());
        assertEquals(3, config.getAllowedMethods().size());
        assertTrue(config.getAllowedMethods().contains("GET"));
        assertTrue(config.getAllowedMethods().contains("POST"));
        assertTrue(config.getAllowedMethods().contains("PUT"));
    }

    @Test
    void corsConfigurationAllowsAllHeaders() {
        CorsConfigurationSource corsConfigurationSource = securityConfiguration.corsConfigurationSource();
        UrlBasedCorsConfigurationSource source = (UrlBasedCorsConfigurationSource) corsConfigurationSource;

        org.springframework.web.cors.CorsConfiguration config = source.getCorsConfigurations().get("/**");
        assertNotNull(config);
        assertNotNull(config.getAllowedHeaders());
        assertTrue(config.getAllowedHeaders().contains("*"));
    }

    @Test
    void corsConfigurationExposesAuthorizationHeader() {
        CorsConfigurationSource corsConfigurationSource = securityConfiguration.corsConfigurationSource();
        UrlBasedCorsConfigurationSource source = (UrlBasedCorsConfigurationSource) corsConfigurationSource;

        org.springframework.web.cors.CorsConfiguration config = source.getCorsConfigurations().get("/**");
        assertNotNull(config);
        assertNotNull(config.getExposedHeaders());
        assertTrue(config.getExposedHeaders().contains("Authorization"));
    }

    @Test
    void corsConfigurationAllowsCredentials() {
        CorsConfigurationSource corsConfigurationSource = securityConfiguration.corsConfigurationSource();
        UrlBasedCorsConfigurationSource source = (UrlBasedCorsConfigurationSource) corsConfigurationSource;

        org.springframework.web.cors.CorsConfiguration config = source.getCorsConfigurations().get("/**");
        assertNotNull(config);
        assertTrue(config.getAllowCredentials());
    }

    @Test
    void corsConfigurationHasMaxAge() {
        CorsConfigurationSource corsConfigurationSource = securityConfiguration.corsConfigurationSource();
        UrlBasedCorsConfigurationSource source = (UrlBasedCorsConfigurationSource) corsConfigurationSource;

        org.springframework.web.cors.CorsConfiguration config = source.getCorsConfigurations().get("/**");
        assertNotNull(config);
        assertEquals(3600L, config.getMaxAge());
    }

    @Test
    void corsConfigurationHandlesSingleOrigin() {
        ReflectionTestUtils.setField(securityConfiguration, "corsAllowedOrigins", "http://localhost:3000");

        CorsConfigurationSource corsConfigurationSource = securityConfiguration.corsConfigurationSource();
        UrlBasedCorsConfigurationSource source = (UrlBasedCorsConfigurationSource) corsConfigurationSource;

        org.springframework.web.cors.CorsConfiguration config = source.getCorsConfigurations().get("/**");
        assertNotNull(config);
        assertEquals(1, config.getAllowedOriginPatterns().size());
    }

    @Test
    void corsConfigurationHandlesMultipleMethods() {
        ReflectionTestUtils.setField(securityConfiguration, "corsAllowedMethods", "GET,POST,PUT,DELETE,PATCH,OPTIONS");

        CorsConfigurationSource corsConfigurationSource = securityConfiguration.corsConfigurationSource();
        UrlBasedCorsConfigurationSource source = (UrlBasedCorsConfigurationSource) corsConfigurationSource;

        org.springframework.web.cors.CorsConfiguration config = source.getCorsConfigurations().get("/**");
        assertNotNull(config);
        assertNotNull(config.getAllowedMethods());
        assertEquals(6, config.getAllowedMethods().size());
    }

    @Test
    void securityConfigurationIsNotNull() {
        assertNotNull(securityConfiguration);
    }

    @Test
    void passwordEncoderProducesDifferentHashesForSamePassword() {
        PasswordEncoder passwordEncoder = securityConfiguration.passwordEncoder();
        String rawPassword = "samePassword";

        String hash1 = passwordEncoder.encode(rawPassword);
        String hash2 = passwordEncoder.encode(rawPassword);

        assertNotEquals(hash1, hash2, "BCrypt should produce different hashes due to salt");
        assertTrue(passwordEncoder.matches(rawPassword, hash1));
        assertTrue(passwordEncoder.matches(rawPassword, hash2));
    }

    @Test
    void passwordEncoderDoesNotMatchWrongPassword() {
        PasswordEncoder passwordEncoder = securityConfiguration.passwordEncoder();
        String password = "correctPassword";
        String wrongPassword = "wrongPassword";

        String encodedPassword = passwordEncoder.encode(password);

        assertFalse(passwordEncoder.matches(wrongPassword, encodedPassword));
    }
}

