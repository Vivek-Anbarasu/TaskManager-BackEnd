package com.restapp.service;

import com.restapp.dao.UserInfoRepository;
import com.restapp.entity.UserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final UserInfoRepository repository;
    private final PasswordEncoder passwordEncoder;

    
    public Optional<UserInfo> findByEmail(String name) {
    	return repository.findByEmail(name);
    }

    public String addUser(UserInfo userInfo) {
        userInfo.setPassword(passwordEncoder.encode(userInfo.getPassword()));
        repository.save(userInfo);
        return "User Succesfully Registered";
    }
}
