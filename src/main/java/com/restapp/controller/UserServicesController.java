package com.restapp.controller;

import com.restapp.dto.AuthenticationRequest;
import com.restapp.entity.UserInfo;
import com.restapp.service.JWTService;
import com.restapp.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RequestMapping("/user")
@Slf4j
@RequiredArgsConstructor
public class UserServicesController {

    private final RegistrationService registrationService;
    private final JWTService jwtService;
    private final AuthenticationManager authenticationManager;

    @PostMapping(path = "/new-registration", produces = MediaType.APPLICATION_JSON_VALUE)
    public String addNewUser(@RequestBody UserInfo userInfo) {

        log.info("Registering email: " + userInfo.getEmail());

    	Optional<UserInfo> optuserInfo = registrationService.findByEmail(userInfo.getEmail());

    	if(optuserInfo.isPresent()) {
    		return "Email already registered, please use a different email";
    	}

        return registrationService.addUser(userInfo);
    }

    @PostMapping(path = "/authenticate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> authenticate(@RequestBody AuthenticationRequest authRequest) {

    	System.out.println("Authenticate request recieved for "+authRequest.email());
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(authRequest.email(), authRequest.password()));

        String jwtToken = jwtService.generateToken(authRequest.email());
        String name = "";
        if(jwtToken != null){
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(jwtToken);

            Optional<UserInfo> optuserInfo = registrationService.findByEmail(authRequest.email());

            if(optuserInfo.isPresent()) {
                name = optuserInfo.get().getFirstname() +" "+ optuserInfo.get().getLastname();
            }

            return new ResponseEntity<>(name,headers, HttpStatus.OK);
        }else{
            return new ResponseEntity<>("Email/Password is not valid",HttpStatus.FORBIDDEN);
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
