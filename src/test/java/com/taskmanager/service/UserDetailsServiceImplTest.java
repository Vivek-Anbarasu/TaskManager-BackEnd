package com.taskmanager.service;

import com.taskmanager.config.UserInfoUserDetails;
import com.taskmanager.domain.model.UserInfo;
import com.taskmanager.domain.repository.UserInfoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserInfoRepository repository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void loadUserByUsernameReturnsUserDetailsWhenEmailExists() {
        UserInfo userInfo = new UserInfo();
        userInfo.setId(1);
        userInfo.setEmail("user@example.com");
        userInfo.setPassword("password123");
        userInfo.setRole("ROLE_USER");
        userInfo.setFirstname("John");
        userInfo.setLastname("Doe");

        when(repository.findByEmail("user@example.com")).thenReturn(Optional.of(userInfo));

        UserDetails result = userDetailsService.loadUserByUsername("user@example.com");

        assertNotNull(result);
        assertEquals("user@example.com", result.getUsername());
        assertEquals("password123", result.getPassword());
        assertNotNull(result.getAuthorities());
        verify(repository, times(1)).findByEmail("user@example.com");
    }

    @Test
    void loadUserByUsernameThrowsExceptionWhenEmailDoesNotExist() {
        when(repository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(
            UsernameNotFoundException.class,
            () -> userDetailsService.loadUserByUsername("nonexistent@example.com")
        );

        assertEquals("Email not found nonexistent@example.com", exception.getMessage());
        verify(repository, times(1)).findByEmail("nonexistent@example.com");
    }

    @Test
    void loadUserByUsernameReturnsUserDetailsWithMultipleRoles() {
        UserInfo userInfo = new UserInfo();
        userInfo.setId(2);
        userInfo.setEmail("admin@example.com");
        userInfo.setPassword("adminpass");
        userInfo.setRole("ROLE_USER,ROLE_ADMIN");

        when(repository.findByEmail("admin@example.com")).thenReturn(Optional.of(userInfo));

        UserDetails result = userDetailsService.loadUserByUsername("admin@example.com");

        assertNotNull(result);
        assertEquals("admin@example.com", result.getUsername());
        assertEquals(2, result.getAuthorities().size());
        verify(repository, times(1)).findByEmail("admin@example.com");
    }

    @Test
    void loadUserByUsernameHandlesNullEmail() {
        when(repository.findByEmail(null)).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(
            UsernameNotFoundException.class,
            () -> userDetailsService.loadUserByUsername(null)
        );

        assertEquals("Email not found null", exception.getMessage());
        verify(repository, times(1)).findByEmail(null);
    }

    @Test
    void loadUserByUsernameHandlesEmptyEmail() {
        when(repository.findByEmail("")).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(
            UsernameNotFoundException.class,
            () -> userDetailsService.loadUserByUsername("")
        );

        assertEquals("Email not found ", exception.getMessage());
        verify(repository, times(1)).findByEmail("");
    }

    @Test
    void loadUserByUsernameReturnsUserDetailsInstanceOfUserInfoUserDetails() {
        UserInfo userInfo = new UserInfo();
        userInfo.setEmail("test@example.com");
        userInfo.setPassword("testpass");
        userInfo.setRole("ROLE_USER");

        when(repository.findByEmail("test@example.com")).thenReturn(Optional.of(userInfo));

        UserDetails result = userDetailsService.loadUserByUsername("test@example.com");

        assertInstanceOf(UserInfoUserDetails.class, result);
        verify(repository, times(1)).findByEmail("test@example.com");
    }

    @Test
    void loadUserByUsernameHandlesEmailWithSpecialCharacters() {
        String specialEmail = "user+test@example.co.uk";
        UserInfo userInfo = new UserInfo();
        userInfo.setEmail(specialEmail);
        userInfo.setPassword("password");
        userInfo.setRole("ROLE_USER");

        when(repository.findByEmail(specialEmail)).thenReturn(Optional.of(userInfo));

        UserDetails result = userDetailsService.loadUserByUsername(specialEmail);

        assertNotNull(result);
        assertEquals(specialEmail, result.getUsername());
        verify(repository, times(1)).findByEmail(specialEmail);
    }

    @Test
    void loadUserByUsernameHandlesEmailWithDifferentCase() {
        String email = "User@Example.COM";
        UserInfo userInfo = new UserInfo();
        userInfo.setEmail(email);
        userInfo.setPassword("password");
        userInfo.setRole("ROLE_USER");

        when(repository.findByEmail(email)).thenReturn(Optional.of(userInfo));

        UserDetails result = userDetailsService.loadUserByUsername(email);

        assertNotNull(result);
        assertEquals(email, result.getUsername());
        verify(repository, times(1)).findByEmail(email);
    }
}

