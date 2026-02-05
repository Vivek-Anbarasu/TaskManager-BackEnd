package com.taskmanager.controller;

import com.taskmanager.api.controller.UserServicesController;
import com.taskmanager.api.dto.*;
import com.taskmanager.domain.model.UserInfo;
import com.taskmanager.mapper.UserMapper;
import com.taskmanager.service.JWTService;
import com.taskmanager.service.RegistrationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServicesControllerTest {

    @Mock
    private RegistrationService registrationService;

    @Mock
    private JWTService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private Authentication authentication;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServicesController userServicesController;

    @Test
    void addNewUserSuccessfullyRegistersNewUser() {
        UserRegistrationRequest userReq = new UserRegistrationRequest("newuser@example.com", "password123", "USA", "ROLE_USER", "John", "Doe");
        UserInfo userInfo = new UserInfo();
        userInfo.setEmail("newuser@example.com");

        when(registrationService.findByEmail("newuser@example.com")).thenReturn(Optional.empty());
        when(userMapper.toUserInfo(userReq)).thenReturn(userInfo);
        when(registrationService.addUser(any(UserInfo.class))).thenReturn("User registered successfully");

        String result = userServicesController.addNewUser(userReq);

        assertEquals("User registered successfully", result);
        verify(registrationService, times(1)).findByEmail("newuser@example.com");
        verify(userMapper, times(1)).toUserInfo(userReq);
        verify(registrationService, times(1)).addUser(any(UserInfo.class));
    }

    @Test
    void addNewUserReturnsErrorMessageWhenEmailAlreadyExists() {
        UserRegistrationRequest userReq = new UserRegistrationRequest("existing@example.com", "password123", null, null, null, null);

        UserInfo existingUser = new UserInfo();
        existingUser.setEmail("existing@example.com");

        when(registrationService.findByEmail("existing@example.com")).thenReturn(Optional.of(existingUser));

        assertThrows(com.taskmanager.exception.BadRequest.class, () -> userServicesController.addNewUser(userReq));
        verify(registrationService, times(1)).findByEmail("existing@example.com");
        verify(registrationService, never()).addUser(any());
    }

    @Test
    void addNewUserWithValidEmailAndRequiredFields() {
        UserRegistrationRequest userReq = new UserRegistrationRequest("valid@example.com", "password123", "USA", "ROLE_USER", "First", "Last");
        UserInfo userInfo = new UserInfo();
        userInfo.setEmail("valid@example.com");

        when(registrationService.findByEmail("valid@example.com")).thenReturn(Optional.empty());
        when(userMapper.toUserInfo(userReq)).thenReturn(userInfo);
        when(registrationService.addUser(any(UserInfo.class))).thenReturn("User registered successfully");

        String result = userServicesController.addNewUser(userReq);

        assertEquals("User registered successfully", result);
        verify(registrationService, times(1)).findByEmail("valid@example.com");
        verify(userMapper, times(1)).toUserInfo(userReq);
    }

    @Test
    void authenticateSuccessfullyReturnsNameAndTokenInHeaders() {
        AuthenticationRequest authRequest = new AuthenticationRequest("user@example.com", "password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getAuthorities()).thenReturn(java.util.Collections.emptyList());
        lenient().when(jwtService.generateToken(anyString(), any())).thenReturn("jwt-token-12345");

        ResponseEntity<?> result = userServicesController.authenticate(authRequest);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("user@example.com", result.getBody());
        assertNotNull(result.getHeaders().get(HttpHeaders.AUTHORIZATION));
        assertEquals("Bearer jwt-token-12345", result.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService, times(1)).generateToken(anyString(), any());
    }

    @Test
    void authenticateReturnsOnlyFirstnameWhenLastnameIsNull() {
        AuthenticationRequest authRequest = new AuthenticationRequest("user@example.com", "password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getAuthorities()).thenReturn(java.util.Collections.emptyList());
        lenient().when(jwtService.generateToken(anyString(), any())).thenReturn("jwt-token-12345");

        ResponseEntity<?> result = userServicesController.authenticate(authRequest);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("user@example.com", result.getBody());
    }

    @Test
    void authenticateReturnsEmptyStringWhenUserNotFoundAfterAuthentication() {
        AuthenticationRequest authRequest = new AuthenticationRequest("user@example.com", "password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getAuthorities()).thenReturn(java.util.Collections.emptyList());
        lenient().when(jwtService.generateToken(anyString(), any())).thenReturn("jwt-token-12345");

        ResponseEntity<?> result = userServicesController.authenticate(authRequest);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("user@example.com", result.getBody());
        assertNotNull(result.getHeaders().get(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void authenticateReturnsForbiddenWhenTokenGenerationFails() {
        AuthenticationRequest authRequest = new AuthenticationRequest("user@example.com", "password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getAuthorities()).thenReturn(java.util.Collections.emptyList());
        lenient().when(jwtService.generateToken(anyString(), any())).thenReturn(null);

        assertThrows(com.taskmanager.exception.Unauthorized.class, () -> userServicesController.authenticate(authRequest));
    }

    @Test
    void authenticateThrowsExceptionWhenCredentialsAreInvalid() {
        AuthenticationRequest authRequest = new AuthenticationRequest("user@example.com", "wrongpassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        assertThrows(BadCredentialsException.class, () -> userServicesController.authenticate(authRequest));
        verify(jwtService, never()).generateToken(anyString(), any());
    }

    @Test
    void authenticateHandlesEmptyEmail() {
        AuthenticationRequest authRequest = new AuthenticationRequest("", "password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getAuthorities()).thenReturn(java.util.Collections.emptyList());
        lenient().when(jwtService.generateToken(anyString(), any())).thenReturn("jwt-token-12345");

        ResponseEntity<?> result = userServicesController.authenticate(authRequest);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void authenticateHandlesEmptyPassword() {
        AuthenticationRequest authRequest = new AuthenticationRequest("user@example.com", "");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getAuthorities()).thenReturn(java.util.Collections.emptyList());
        lenient().when(jwtService.generateToken(anyString(), any())).thenReturn("jwt-token-12345");

        ResponseEntity<?> result = userServicesController.authenticate(authRequest);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void authenticateCreatesCorrectAuthenticationToken() {
        AuthenticationRequest authRequest = new AuthenticationRequest("test@example.com", "testpass");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getAuthorities()).thenReturn(java.util.Collections.emptyList());
        lenient().when(jwtService.generateToken(anyString(), any())).thenReturn("valid-token");

        ResponseEntity<?> result = userServicesController.authenticate(authRequest);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(authenticationManager).authenticate(argThat(token ->
            token instanceof UsernamePasswordAuthenticationToken &&
            token.getPrincipal().equals("test@example.com") &&
            token.getCredentials().equals("testpass")
        ));
    }

    @Test
    void addNewUserWithCompleteUserInformation() {
        UserRegistrationRequest userReq = new UserRegistrationRequest("complete@example.com", "securePassword", "USA", "USER", "Complete", "User");
        UserInfo userInfo = new UserInfo();
        userInfo.setEmail("complete@example.com");

        when(registrationService.findByEmail("complete@example.com")).thenReturn(Optional.empty());
        when(userMapper.toUserInfo(userReq)).thenReturn(userInfo);
        when(registrationService.addUser(any(UserInfo.class))).thenReturn("Registration successful");

        String result = userServicesController.addNewUser(userReq);

        assertEquals("Registration successful", result);
        verify(userMapper, times(1)).toUserInfo(userReq);
        verify(registrationService, times(1)).addUser(any(UserInfo.class));
    }

    @Test
    void refreshTokenSuccessfullyGeneratesNewToken() {
        String oldToken = "eyJhbGciOiJIUzUxMiJ9.oldtoken";
        String authHeader = "Bearer " + oldToken;

        UserInfo userInfo = new UserInfo();
        userInfo.setEmail("user@example.com");

        when(jwtService.extractEmail(oldToken)).thenReturn("user@example.com");
        when(jwtService.extractRole(oldToken)).thenReturn("ROLE_USER");
        when(registrationService.findByEmail("user@example.com")).thenReturn(Optional.of(userInfo));
        when(jwtService.validateToken("user@example.com", "user@example.com", oldToken)).thenReturn(true);
        when(jwtService.generateToken(eq("user@example.com"), any())).thenReturn("new-jwt-token");

        ResponseEntity<?> result = userServicesController.refreshToken(authHeader);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("user@example.com", result.getBody());
        assertEquals("Bearer new-jwt-token", result.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        verify(jwtService, times(1)).extractEmail(oldToken);
        verify(jwtService, times(1)).extractRole(oldToken);
        verify(jwtService, times(1)).validateToken("user@example.com", "user@example.com", oldToken);
        verify(jwtService, times(1)).generateToken(eq("user@example.com"), eq(java.util.Map.of("role", "ROLE_USER")));
    }

    @Test
    void refreshTokenThrowsUnauthorizedWhenHeaderIsMissing() {
        assertThrows(com.taskmanager.exception.Unauthorized.class,
                () -> userServicesController.refreshToken(null));
    }

    @Test
    void refreshTokenThrowsUnauthorizedWhenHeaderDoesNotStartWithBearer() {
        String invalidHeader = "Basic abc123";

        assertThrows(com.taskmanager.exception.Unauthorized.class,
                () -> userServicesController.refreshToken(invalidHeader));
    }

    @Test
    void refreshTokenThrowsUnauthorizedWhenUserNotFound() {
        String oldToken = "eyJhbGciOiJIUzUxMiJ9.oldtoken";
        String authHeader = "Bearer " + oldToken;

        when(jwtService.extractEmail(oldToken)).thenReturn("nonexistent@example.com");
        when(jwtService.extractRole(oldToken)).thenReturn("ROLE_USER");
        when(registrationService.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThrows(com.taskmanager.exception.Unauthorized.class,
                () -> userServicesController.refreshToken(authHeader));

        verify(registrationService, times(1)).findByEmail("nonexistent@example.com");
        verify(jwtService, never()).validateToken(anyString(), anyString(), anyString());
    }

    @Test
    void refreshTokenThrowsUnauthorizedWhenTokenValidationFails() {
        String oldToken = "eyJhbGciOiJIUzUxMiJ9.expiredtoken";
        String authHeader = "Bearer " + oldToken;

        UserInfo userInfo = new UserInfo();
        userInfo.setEmail("user@example.com");

        when(jwtService.extractEmail(oldToken)).thenReturn("user@example.com");
        when(jwtService.extractRole(oldToken)).thenReturn("ROLE_USER");
        when(registrationService.findByEmail("user@example.com")).thenReturn(Optional.of(userInfo));
        when(jwtService.validateToken("user@example.com", "user@example.com", oldToken)).thenReturn(false);

        assertThrows(com.taskmanager.exception.Unauthorized.class,
                () -> userServicesController.refreshToken(authHeader));

        verify(jwtService, times(1)).validateToken("user@example.com", "user@example.com", oldToken);
        verify(jwtService, never()).generateToken(anyString(), any());
    }

    @Test
    void refreshTokenThrowsUnauthorizedWhenNewTokenGenerationFails() {
        String oldToken = "eyJhbGciOiJIUzUxMiJ9.oldtoken";
        String authHeader = "Bearer " + oldToken;

        UserInfo userInfo = new UserInfo();
        userInfo.setEmail("user@example.com");

        when(jwtService.extractEmail(oldToken)).thenReturn("user@example.com");
        when(jwtService.extractRole(oldToken)).thenReturn("ROLE_USER");
        when(registrationService.findByEmail("user@example.com")).thenReturn(Optional.of(userInfo));
        when(jwtService.validateToken("user@example.com", "user@example.com", oldToken)).thenReturn(true);
        when(jwtService.generateToken(anyString(), any())).thenReturn(null);

        assertThrows(com.taskmanager.exception.Unauthorized.class,
                () -> userServicesController.refreshToken(authHeader));
    }

    @Test
    void refreshTokenPreservesUserRoleInNewToken() {
        String oldToken = "eyJhbGciOiJIUzUxMiJ9.admintoken";
        String authHeader = "Bearer " + oldToken;

        UserInfo userInfo = new UserInfo();
        userInfo.setEmail("admin@example.com");

        when(jwtService.extractEmail(oldToken)).thenReturn("admin@example.com");
        when(jwtService.extractRole(oldToken)).thenReturn("ROLE_ADMIN");
        when(registrationService.findByEmail("admin@example.com")).thenReturn(Optional.of(userInfo));
        when(jwtService.validateToken("admin@example.com", "admin@example.com", oldToken)).thenReturn(true);
        when(jwtService.generateToken(anyString(), any())).thenReturn("new-admin-token");

        ResponseEntity<?> result = userServicesController.refreshToken(authHeader);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(jwtService).generateToken(eq("admin@example.com"), eq(java.util.Map.of("role", "ROLE_ADMIN")));
    }

    @Test
    void refreshTokenHandlesMalformedToken() {
        String authHeader = "Bearer malformed.token";

        when(jwtService.extractEmail("malformed.token")).thenThrow(new RuntimeException("Invalid token format"));

        assertThrows(com.taskmanager.exception.Unauthorized.class,
                () -> userServicesController.refreshToken(authHeader));
    }

    @Test
    void refreshTokenHandlesEmptyAuthorizationHeader() {
        String authHeader = "";

        assertThrows(com.taskmanager.exception.Unauthorized.class,
                () -> userServicesController.refreshToken(authHeader));
    }

    @Test
    void refreshTokenHandlesBearerWithoutToken() {
        String authHeader = "Bearer ";

        assertThrows(com.taskmanager.exception.Unauthorized.class,
                () -> userServicesController.refreshToken(authHeader));
    }
}
