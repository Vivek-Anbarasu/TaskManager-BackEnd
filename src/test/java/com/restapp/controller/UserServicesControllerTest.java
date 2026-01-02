package com.restapp.controller;

import com.restapp.dto.AuthenticationRequest;
import com.restapp.entity.UserInfo;
import com.restapp.service.JWTService;
import com.restapp.service.RegistrationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
class UserServicesControllerTest {

    @Mock
    private RegistrationService registrationService;

    @Mock
    private JWTService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserServicesController userServicesController;

    @Test
    void addNewUserSuccessfullyRegistersNewUser() {
        UserInfo userInfo = new UserInfo();
        userInfo.setEmail("newuser@example.com");
        userInfo.setPassword("password123");
        userInfo.setFirstname("John");
        userInfo.setLastname("Doe");

        when(registrationService.findByEmail("newuser@example.com")).thenReturn(Optional.empty());
        when(registrationService.addUser(userInfo)).thenReturn("User registered successfully");

        String result = userServicesController.addNewUser(userInfo);

        assertEquals("User registered successfully", result);
        verify(registrationService, times(1)).findByEmail("newuser@example.com");
        verify(registrationService, times(1)).addUser(userInfo);
    }

    @Test
    void addNewUserReturnsErrorMessageWhenEmailAlreadyExists() {
        UserInfo userInfo = new UserInfo();
        userInfo.setEmail("existing@example.com");
        userInfo.setPassword("password123");

        UserInfo existingUser = new UserInfo();
        existingUser.setEmail("existing@example.com");

        when(registrationService.findByEmail("existing@example.com")).thenReturn(Optional.of(existingUser));

        String result = userServicesController.addNewUser(userInfo);

        assertEquals("Email already registered, please use a different email", result);
        verify(registrationService, times(1)).findByEmail("existing@example.com");
        verify(registrationService, never()).addUser(any());
    }

    @Test
    void addNewUserHandlesNullEmail() {
        UserInfo userInfo = new UserInfo();
        userInfo.setEmail(null);
        userInfo.setPassword("password123");

        when(registrationService.findByEmail(null)).thenReturn(Optional.empty());
        when(registrationService.addUser(userInfo)).thenReturn("User registered successfully");

        String result = userServicesController.addNewUser(userInfo);

        assertEquals("User registered successfully", result);
        verify(registrationService, times(1)).findByEmail(null);
    }

    @Test
    void authenticateSuccessfullyReturnsNameAndTokenInHeaders() {
        AuthenticationRequest authRequest = new AuthenticationRequest("user@example.com", "password123");

        UserInfo userInfo = new UserInfo();
        userInfo.setEmail("user@example.com");
        userInfo.setFirstname("Jane");
        userInfo.setLastname("Smith");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(jwtService.generateToken("user@example.com")).thenReturn("jwt-token-12345");
        when(registrationService.findByEmail("user@example.com")).thenReturn(Optional.of(userInfo));

        ResponseEntity<?> result = userServicesController.authenticate(authRequest);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("Jane Smith", result.getBody());
        assertNotNull(result.getHeaders().get(HttpHeaders.AUTHORIZATION));
        assertEquals("Bearer jwt-token-12345", result.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService, times(1)).generateToken("user@example.com");
    }

    @Test
    void authenticateReturnsOnlyFirstnameWhenLastnameIsNull() {
        AuthenticationRequest authRequest = new AuthenticationRequest("user@example.com", "password123");

        UserInfo userInfo = new UserInfo();
        userInfo.setEmail("user@example.com");
        userInfo.setFirstname("Jane");
        userInfo.setLastname(null);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(jwtService.generateToken("user@example.com")).thenReturn("jwt-token-12345");
        when(registrationService.findByEmail("user@example.com")).thenReturn(Optional.of(userInfo));

        ResponseEntity<?> result = userServicesController.authenticate(authRequest);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("Jane null", result.getBody());
    }

    @Test
    void authenticateReturnsEmptyStringWhenUserNotFoundAfterAuthentication() {
        AuthenticationRequest authRequest = new AuthenticationRequest("user@example.com", "password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(jwtService.generateToken("user@example.com")).thenReturn("jwt-token-12345");
        when(registrationService.findByEmail("user@example.com")).thenReturn(Optional.empty());

        ResponseEntity<?> result = userServicesController.authenticate(authRequest);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("", result.getBody());
        assertNotNull(result.getHeaders().get(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void authenticateReturnsForbiddenWhenTokenGenerationFails() {
        AuthenticationRequest authRequest = new AuthenticationRequest("user@example.com", "password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(jwtService.generateToken("user@example.com")).thenReturn(null);

        ResponseEntity<?> result = userServicesController.authenticate(authRequest);

        assertEquals(HttpStatus.FORBIDDEN, result.getStatusCode());
        assertEquals("Email/Password is not valid", result.getBody());
        verify(registrationService, never()).findByEmail(anyString());
    }

    @Test
    void authenticateThrowsExceptionWhenCredentialsAreInvalid() {
        AuthenticationRequest authRequest = new AuthenticationRequest("user@example.com", "wrongpassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        assertThrows(BadCredentialsException.class, () -> userServicesController.authenticate(authRequest));
        verify(jwtService, never()).generateToken(anyString());
    }

    @Test
    void authenticateHandlesEmptyEmail() {
        AuthenticationRequest authRequest = new AuthenticationRequest("", "password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(jwtService.generateToken("")).thenReturn("jwt-token-12345");
        when(registrationService.findByEmail("")).thenReturn(Optional.empty());

        ResponseEntity<?> result = userServicesController.authenticate(authRequest);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void authenticateHandlesEmptyPassword() {
        AuthenticationRequest authRequest = new AuthenticationRequest("user@example.com", "");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(jwtService.generateToken("user@example.com")).thenReturn("jwt-token-12345");
        when(registrationService.findByEmail("user@example.com")).thenReturn(Optional.empty());

        ResponseEntity<?> result = userServicesController.authenticate(authRequest);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void authenticateCreatesCorrectAuthenticationToken() {
        AuthenticationRequest authRequest = new AuthenticationRequest("test@example.com", "testpass");

        UserInfo userInfo = new UserInfo();
        userInfo.setEmail("test@example.com");
        userInfo.setFirstname("Test");
        userInfo.setLastname("User");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(jwtService.generateToken("test@example.com")).thenReturn("valid-token");
        when(registrationService.findByEmail("test@example.com")).thenReturn(Optional.of(userInfo));

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
        UserInfo userInfo = new UserInfo();
        userInfo.setEmail("complete@example.com");
        userInfo.setPassword("securePassword");
        userInfo.setFirstname("Complete");
        userInfo.setLastname("User");
        userInfo.setCountry("USA");
        userInfo.setRoles("USER");

        when(registrationService.findByEmail("complete@example.com")).thenReturn(Optional.empty());
        when(registrationService.addUser(userInfo)).thenReturn("Registration successful");

        String result = userServicesController.addNewUser(userInfo);

        assertEquals("Registration successful", result);
        verify(registrationService, times(1)).addUser(userInfo);
    }
}

