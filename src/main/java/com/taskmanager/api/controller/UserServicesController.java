package com.taskmanager.api.controller;

import com.taskmanager.api.dto.AuthenticationRequest;
import com.taskmanager.api.dto.UserRegistrationRequest;
import com.taskmanager.domain.model.UserInfo;
import com.taskmanager.service.JWTService;
import com.taskmanager.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/user")
@Slf4j
@RequiredArgsConstructor
public class UserServicesController {

    private final RegistrationService registrationService;
    private final JWTService jwtService;
    private final AuthenticationManager authenticationManager;

    @PostMapping(path = "/new-registration", produces = MediaType.APPLICATION_JSON_VALUE)
    public String addNewUser(@RequestBody UserRegistrationRequest userReq) {

        log.info("Registering email: {}", userReq.getEmail());

    	Optional<UserInfo> optuserInfo = registrationService.findByEmail(userReq.getEmail());

    	if(optuserInfo.isPresent()) {
            throw new com.taskmanager.exception.BadRequest("Email already registered, please use a different email");
        }

        UserInfo userInfo = new UserInfo();
        userInfo.setEmail(userReq.getEmail());
        userInfo.setPassword(userReq.getPassword());
        userInfo.setCountry(userReq.getCountry());
        userInfo.setRole(userReq.getRole());
        userInfo.setFirstname(userReq.getFirstname());
        userInfo.setLastname(userReq.getLastname());

        return registrationService.addUser(userInfo);
    }

    @PostMapping(path = "/authenticate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> authenticate(@RequestBody AuthenticationRequest authRequest) {

        log.info("Authenticate request received for {}", authRequest.email());
         Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(authRequest.email(), authRequest.password()));
         // If authentication is successful, use roles from database, prepare claims and generate JWT token
         Set<String> roles = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
         Map<String,String> claims = Map.of("role", String.join(",", roles));
         String jwtToken = jwtService.generateToken(authRequest.email(),claims);
         if(jwtToken != null){
             HttpHeaders headers = new HttpHeaders();
             headers.setBearerAuth(jwtToken);
             return new ResponseEntity<>("Authentication successful for "+authRequest.email(), headers, HttpStatus.OK);
         }else{
            throw new com.taskmanager.exception.Unauthorized("Email/Password is not valid");
         }
    }
    
//    @PostMapping(path = "/refresh", produces = MediaType.APPLICATION_JSON_VALUE)
//    public AuthResponse refreshToken(@RequestHeader("Token") String jwtToken) {
//    	String subject = jwtService.extractEmail(jwtToken);
//    	String creds[] = subject.split(" ");
//    	AuthRequest authRequest = new AuthRequest();
//    	authRequest.setUsername(creds[0]);
//    	authRequest.setPassword(creds[1]);
//    	return authenticateAndGetToken(authRequest);
//    }
}
