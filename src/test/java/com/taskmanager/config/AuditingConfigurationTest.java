package com.taskmanager.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuditingConfigurationTest {

    private AuditingConfiguration configuration;
    private SecurityContext securityContext;

    @BeforeEach
    void setUp() {
        configuration = new AuditingConfiguration();
        securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void auditorProviderBeanIsCreated() {
        AuditorAware<String> auditorAware = configuration.auditorProvider();
        assertNotNull(auditorAware);
        assertInstanceOf(AuditingConfiguration.AuditorAwareImpl.class, auditorAware);
    }

    @Test
    void getCurrentAuditorReturnsAuthenticatedUsername() {
        // Arrange
        String expectedUsername = "test@example.com";
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            expectedUsername, "password", new ArrayList<>()
        );
        when(securityContext.getAuthentication()).thenReturn(authentication);

        AuditingConfiguration.AuditorAwareImpl auditorAware = new AuditingConfiguration.AuditorAwareImpl();

        // Act
        Optional<String> auditor = auditorAware.getCurrentAuditor();

        // Assert
        assertTrue(auditor.isPresent());
        assertEquals(expectedUsername, auditor.get());
    }

    @Test
    void getCurrentAuditorReturnsAnonymousWhenAuthenticationIsNull() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(null);

        AuditingConfiguration.AuditorAwareImpl auditorAware = new AuditingConfiguration.AuditorAwareImpl();

        // Act
        Optional<String> auditor = auditorAware.getCurrentAuditor();

        // Assert
        assertTrue(auditor.isPresent());
        assertEquals("Anonymous User", auditor.get());
    }

    @Test
    void getCurrentAuditorReturnsAnonymousWhenNotAuthenticated() {
        // Arrange
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(false);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        AuditingConfiguration.AuditorAwareImpl auditorAware = new AuditingConfiguration.AuditorAwareImpl();

        // Act
        Optional<String> auditor = auditorAware.getCurrentAuditor();

        // Assert
        assertTrue(auditor.isPresent());
        assertEquals("Anonymous User", auditor.get());
    }

    @Test
    void getCurrentAuditorReturnsAnonymousWhenPrincipalIsAnonymousUser() {
        // Arrange
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("anonymousUser");
        when(securityContext.getAuthentication()).thenReturn(authentication);

        AuditingConfiguration.AuditorAwareImpl auditorAware = new AuditingConfiguration.AuditorAwareImpl();

        // Act
        Optional<String> auditor = auditorAware.getCurrentAuditor();

        // Assert
        assertTrue(auditor.isPresent());
        assertEquals("Anonymous User", auditor.get());
    }

    @Test
    void getCurrentAuditorExtractsEmailFromAuthentication() {
        // Arrange
        String email = "admin@example.com";
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("userObject");
        when(authentication.getName()).thenReturn(email);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        AuditingConfiguration.AuditorAwareImpl auditorAware = new AuditingConfiguration.AuditorAwareImpl();

        // Act
        Optional<String> auditor = auditorAware.getCurrentAuditor();

        // Assert
        assertTrue(auditor.isPresent());
        assertEquals(email, auditor.get());
    }

    @Test
    void getCurrentAuditorAlwaysReturnsOptionalWithValue() {
        // Test with null authentication
        when(securityContext.getAuthentication()).thenReturn(null);
        AuditingConfiguration.AuditorAwareImpl auditorAware = new AuditingConfiguration.AuditorAwareImpl();

        Optional<String> auditor = auditorAware.getCurrentAuditor();

        assertTrue(auditor.isPresent());
        assertNotNull(auditor.get());
    }

    @Test
    void multipleCallsToGetCurrentAuditorAreConsistent() {
        // Arrange
        String email = "consistent@example.com";
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            email, "password", new ArrayList<>()
        );
        when(securityContext.getAuthentication()).thenReturn(authentication);

        AuditingConfiguration.AuditorAwareImpl auditorAware = new AuditingConfiguration.AuditorAwareImpl();

        // Act
        Optional<String> auditor1 = auditorAware.getCurrentAuditor();
        Optional<String> auditor2 = auditorAware.getCurrentAuditor();

        // Assert
        assertEquals(auditor1, auditor2);
        assertTrue(auditor1.isPresent());
        assertTrue(auditor2.isPresent());
        assertEquals(email, auditor1.orElse(null));
        assertEquals(email, auditor2.orElse(null));
    }
}

