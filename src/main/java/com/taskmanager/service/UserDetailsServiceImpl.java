package com.taskmanager.service;

import com.taskmanager.security.UserInfoUserDetails;
import com.taskmanager.domain.repository.UserInfoRepository;
import com.taskmanager.domain.model.UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserInfoRepository repository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Optional<UserInfo> userInfo = repository.findByEmail(email);
        if (userInfo.isEmpty()) {
            log.warn("User not found for email={}", email);
        }
        return userInfo.map(UserInfoUserDetails::new).orElseThrow(() -> new UsernameNotFoundException("Email not found " + email));

    }
}
