package com.taskmanager.controller;

import com.taskmanager.api.controller.UserServicesController;
import com.taskmanager.api.dto.*;
import com.taskmanager.domain.model.UserInfo;
import com.taskmanager.service.JWTService;
import com.taskmanager.service.RegistrationService;
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
        UserRegistrationRequest userReq = new UserRegistrationRequest("newuser@example.com", "password123", null, null, "John", "Doe");

        when(registrationService.findByEmail("newuser@example.com")).thenReturn(Optional.empty());
        when(registrationService.addUser(any(UserInfo.class))).thenReturn("User registered successfully");

        String result = userServicesController.addNewUser(userReq);

        assertEquals("User registered successfully", result);
        verify(registrationService, times(1)).findByEmail("newuser@example.com");
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
    void addNewUserHandlesNullEmail() {
        UserRegistrationRequest userReq = new UserRegistrationRequest(null, "password123", null, null, null, null);

        when(registrationService.findByEmail(null)).thenReturn(Optional.empty());
        when(registrationService.addUser(any(UserInfo.class))).thenReturn("User registered successfully");

        String result = userServicesController.addNewUser(userReq);

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
        when(jwtService.generateToken(eq("user@example.com"), any())).thenReturn("jwt-token-12345");
        when(registrationService.findByEmail("user@example.com")).thenReturn(Optional.of(userInfo));

        ResponseEntity<?> result = userServicesController.authenticate(authRequest);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("Jane Smith", result.getBody());
        assertNotNull(result.getHeaders().get(HttpHeaders.AUTHORIZATION));
        assertEquals("Bearer jwt-token-12345", result.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService, times(1)).generateToken(eq("user@example.com"), any());
    }

    @Test
    void authenticateReturnsOnlyFirstnameWhenLastnameIsNull() {
        AuthenticationRequest authRequest = new AuthenticationRequest("user@example.com", "password123");

        UserInfo userInfo = new UserInfo();
        userInfo.setEmail("user@example.com");
        userInfo.setFirstname("Jane");
        userInfo.setLastname(null);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(jwtService.generateToken(eq("user@example.com"), any())).thenReturn("jwt-token-12345");
        when(registrationService.findByEmail("user@example.com")).thenReturn(Optional.of(userInfo));

        ResponseEntity<?> result = userServicesController.authenticate(authRequest);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("Jane null", result.getBody());
    }

    @Test
    void authenticateReturnsEmptyStringWhenUserNotFoundAfterAuthentication() {
        AuthenticationRequest authRequest = new AuthenticationRequest("user@example.com", "password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(jwtService.generateToken(eq("user@example.com"), any())).thenReturn("jwt-token-12345");
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
        when(jwtService.generateToken(eq("user@example.com"), any())).thenReturn(null);

        assertThrows(com.taskmanager.exception.Unauthorized.class, () -> userServicesController.authenticate(authRequest));
        verify(registrationService, never()).findByEmail(anyString());
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
        when(jwtService.generateToken(eq(""), any())).thenReturn("jwt-token-12345");
        when(registrationService.findByEmail("")).thenReturn(Optional.empty());

        ResponseEntity<?> result = userServicesController.authenticate(authRequest);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void authenticateHandlesEmptyPassword() {
        AuthenticationRequest authRequest = new AuthenticationRequest("user@example.com", "");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(jwtService.generateToken(eq("user@example.com"), any())).thenReturn("jwt-token-12345");
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
        when(jwtService.generateToken(eq("test@example.com"), any())).thenReturn("valid-token");
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
        UserRegistrationRequest userReq = new UserRegistrationRequest("complete@example.com", "securePassword", "USA", "USER", "Complete", "User");

        when(registrationService.findByEmail("complete@example.com")).thenReturn(Optional.empty());
        when(registrationService.addUser(any(UserInfo.class))).thenReturn("Registration successful");

        String result = userServicesController.addNewUser(userReq);

        assertEquals("Registration successful", result);
        verify(registrationService, times(1)).addUser(any(UserInfo.class));
    }
}
