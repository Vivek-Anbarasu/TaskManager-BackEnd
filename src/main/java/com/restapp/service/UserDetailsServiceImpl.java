package com.restapp.service;

import com.restapp.config.UserInfoUserDetails;
import com.restapp.dao.UserInfoRepository;
import com.restapp.entity.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserInfoRepository repository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Optional<UserInfo> userInfo = repository.findByEmail(email);
        return userInfo.map(UserInfoUserDetails::new).orElseThrow(() -> new UsernameNotFoundException("Email not found " + email));

    }
}
