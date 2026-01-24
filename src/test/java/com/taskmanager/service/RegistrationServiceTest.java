package com.taskmanager.service;



import com.taskmanager.domain.model.UserInfo;
import com.taskmanager.domain.repository.UserInfoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private UserInfoRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private RegistrationService registrationService;

    @Test
    void findByEmailReturnsUserWhenEmailExists() {
        UserInfo userInfo = new UserInfo();
        userInfo.setId(1);
        userInfo.setEmail("user@example.com");
        userInfo.setPassword("password123");

        when(repository.findByEmail("user@example.com")).thenReturn(Optional.of(userInfo));

        Optional<UserInfo> result = registrationService.findByEmail("user@example.com");

        assertTrue(result.isPresent());
        assertEquals("user@example.com", result.get().getEmail());
        verify(repository, times(1)).findByEmail("user@example.com");
    }

    @Test
    void findByEmailReturnsEmptyOptionalWhenEmailDoesNotExist() {
        when(repository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        Optional<UserInfo> result = registrationService.findByEmail("nonexistent@example.com");

        assertFalse(result.isPresent());
        verify(repository, times(1)).findByEmail("nonexistent@example.com");
    }

    @Test
    void findByEmailHandlesNullEmail() {
        when(repository.findByEmail(null)).thenReturn(Optional.empty());

        Optional<UserInfo> result = registrationService.findByEmail(null);

        assertFalse(result.isPresent());
        verify(repository, times(1)).findByEmail(null);
    }

    @Test
    void findByEmailHandlesEmptyString() {
        when(repository.findByEmail("")).thenReturn(Optional.empty());

        Optional<UserInfo> result = registrationService.findByEmail("");

        assertFalse(result.isPresent());
        verify(repository, times(1)).findByEmail("");
    }

    @Test
    void findByEmailHandlesEmailWithSpecialCharacters() {
        String email = "user+test@example.co.uk";
        UserInfo userInfo = new UserInfo();
        userInfo.setEmail(email);

        when(repository.findByEmail(email)).thenReturn(Optional.of(userInfo));

        Optional<UserInfo> result = registrationService.findByEmail(email);

        assertTrue(result.isPresent());
        assertEquals(email, result.get().getEmail());
        verify(repository, times(1)).findByEmail(email);
    }

    @Test
    void addUserEncodesPasswordAndSavesUser() {
        UserInfo userInfo = new UserInfo();
        userInfo.setEmail("newuser@example.com");
        userInfo.setPassword("plainPassword");
        userInfo.setFirstname("John");
        userInfo.setLastname("Doe");

        when(passwordEncoder.encode("plainPassword")).thenReturn("encodedPassword");
        when(repository.save(any(UserInfo.class))).thenReturn(userInfo);

        String result = registrationService.addUser(userInfo);

        assertEquals("User Succesfully Registered", result);
        assertEquals("encodedPassword", userInfo.getPassword());
        verify(passwordEncoder, times(1)).encode("plainPassword");
        verify(repository, times(1)).save(userInfo);
    }

    @Test
    void addUserReturnsSuccessMessage() {
        UserInfo userInfo = new UserInfo();
        userInfo.setEmail("user@example.com");
        userInfo.setPassword("password");

        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(repository.save(any(UserInfo.class))).thenReturn(userInfo);

        String result = registrationService.addUser(userInfo);

        assertEquals("User Succesfully Registered", result);
    }

    @Test
    void addUserEncodesPasswordBeforeSaving() {
        UserInfo userInfo = new UserInfo();
        userInfo.setEmail("test@example.com");
        userInfo.setPassword("myPassword123");

        when(passwordEncoder.encode("myPassword123")).thenReturn("encoded123");
        when(repository.save(any(UserInfo.class))).thenAnswer(invocation -> {
            UserInfo savedUser = invocation.getArgument(0);
            assertEquals("encoded123", savedUser.getPassword());
            return savedUser;
        });

        registrationService.addUser(userInfo);

        verify(passwordEncoder, times(1)).encode("myPassword123");
        verify(repository, times(1)).save(userInfo);
    }

    @Test
    void addUserPreservesOtherUserFields() {
        UserInfo userInfo = new UserInfo();
        userInfo.setEmail("user@example.com");
        userInfo.setPassword("password");
        userInfo.setFirstname("Jane");
        userInfo.setLastname("Smith");
        userInfo.setCountry("USA");
        userInfo.setRoles("ROLE_USER");

        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        when(repository.save(any(UserInfo.class))).thenAnswer(invocation -> {
            UserInfo savedUser = invocation.getArgument(0);
            assertEquals("user@example.com", savedUser.getEmail());
            assertEquals("Jane", savedUser.getFirstname());
            assertEquals("Smith", savedUser.getLastname());
            assertEquals("USA", savedUser.getCountry());
            assertEquals("ROLE_USER", savedUser.getRoles());
            return savedUser;
        });

        registrationService.addUser(userInfo);

        verify(repository, times(1)).save(userInfo);
    }

    @Test
    void addUserHandlesNullPassword() {
        UserInfo userInfo = new UserInfo();
        userInfo.setEmail("user@example.com");
        userInfo.setPassword(null);

        when(passwordEncoder.encode(null)).thenReturn("encodedNull");
        when(repository.save(any(UserInfo.class))).thenReturn(userInfo);

        String result = registrationService.addUser(userInfo);

        assertEquals("User Succesfully Registered", result);
        verify(passwordEncoder, times(1)).encode(null);
    }

    @Test
    void addUserHandlesEmptyPassword() {
        UserInfo userInfo = new UserInfo();
        userInfo.setEmail("user@example.com");
        userInfo.setPassword("");

        when(passwordEncoder.encode("")).thenReturn("encodedEmpty");
        when(repository.save(any(UserInfo.class))).thenReturn(userInfo);

        String result = registrationService.addUser(userInfo);

        assertEquals("User Succesfully Registered", result);
        verify(passwordEncoder, times(1)).encode("");
    }

    @Test
    void addUserModifiesOriginalUserInfoObject() {
        UserInfo userInfo = new UserInfo();
        userInfo.setEmail("user@example.com");
        userInfo.setPassword("originalPassword");

        when(passwordEncoder.encode("originalPassword")).thenReturn("newEncodedPassword");
        when(repository.save(any(UserInfo.class))).thenReturn(userInfo);

        registrationService.addUser(userInfo);

        assertEquals("newEncodedPassword", userInfo.getPassword());
    }

    @Test
    void addUserCallsRepositorySaveAfterPasswordEncoding() {
        UserInfo userInfo = new UserInfo();
        userInfo.setEmail("user@example.com");
        userInfo.setPassword("password123");

        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(repository.save(userInfo)).thenReturn(userInfo);

        registrationService.addUser(userInfo);

        verify(passwordEncoder).encode("password123");
        verify(repository).save(userInfo);
    }

    @Test
    void findByEmailReturnsSameUserInfoFromRepository() {
        UserInfo expectedUser = new UserInfo();
        expectedUser.setId(5);
        expectedUser.setEmail("specific@example.com");
        expectedUser.setFirstname("Test");

        when(repository.findByEmail("specific@example.com")).thenReturn(Optional.of(expectedUser));

        Optional<UserInfo> result = registrationService.findByEmail("specific@example.com");

        assertTrue(result.isPresent());
        assertEquals(expectedUser.getId(), result.get().getId());
        assertEquals(expectedUser.getFirstname(), result.get().getFirstname());
    }
}

