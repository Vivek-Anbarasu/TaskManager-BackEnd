package com.taskmanager.service;

import com.taskmanager.domain.repository.UserInfoRepository;
import com.taskmanager.domain.model.UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationService {

    private final UserInfoRepository repository;
    private final PasswordEncoder passwordEncoder;

    
    public Optional<UserInfo> findByEmail(String name) {
    	return repository.findByEmail(name);
    }

    public String addUser(UserInfo userInfo) {
        userInfo.setPassword(passwordEncoder.encode(userInfo.getPassword()));
        repository.save(userInfo);
        log.info("Registered new user with email={}", userInfo.getEmail());
        return "User Succesfully Registered";
    }
}
