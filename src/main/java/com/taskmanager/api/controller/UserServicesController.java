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
             return new ResponseEntity<>(authRequest.email(), headers, HttpStatus.OK);
         }else{
            throw new com.taskmanager.exception.Unauthorized("Email/Password is not valid");
         }
    }

    @PostMapping(path = "/refresh-token", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> refreshToken(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {

        log.info("Refresh token request received");

        try {
            // Extract token from Authorization header (remove "Bearer " prefix)
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                throw new com.taskmanager.exception.Unauthorized("Invalid Authorization header format. Expected: Bearer <token>");
            }

            String token = authorizationHeader.substring(7); // Remove "Bearer " prefix

            // Extract email and role from the existing token
            String email = jwtService.extractEmail(token);
            String role = jwtService.extractRole(token);

            // Verify the user still exists in the database
            Optional<UserInfo> optUserInfo = registrationService.findByEmail(email);

            if (optUserInfo.isEmpty()) {
                throw new com.taskmanager.exception.Unauthorized("User not found");
            }

            // Validate the token
            if (!jwtService.validateToken(email, optUserInfo.get().getEmail(), token)) {
                throw new com.taskmanager.exception.Unauthorized("Invalid or expired token");
            }

            // Generate new token with the same claims
            Map<String, String> claims = Map.of("role", role);
            String newJwtToken = jwtService.generateToken(email, claims);

            if (newJwtToken != null) {
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(newJwtToken);
                log.info("Token refreshed successfully for email: {}", email);
                return new ResponseEntity<>(email, headers, HttpStatus.OK);
            } else {
                throw new com.taskmanager.exception.Unauthorized("Failed to generate new token");
            }

        } catch (Exception e) {
            log.error("Error refreshing token: {}", e.getMessage());
            throw new com.taskmanager.exception.Unauthorized("Invalid token: " + e.getMessage());
        }
    }


}
