package com.restapp.exception;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
@Hidden
@Slf4j
public class TaskManagementExceptionHandler {
	
	    @ExceptionHandler(MethodArgumentNotValidException.class)
	    @ResponseBody
	    public ResponseEntity<ErrorResponse> processUnmergeException(final MethodArgumentNotValidException ex) {
	       List<String> details = ex.getBindingResult().getAllErrors().stream()
	               .map(fieldError -> fieldError.getDefaultMessage())
	               .collect(Collectors.toList());
	        log.warn("Validation failed: {}", details);
	        ErrorResponse response = new ErrorResponse(400, "Validation Failed", details);
	        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	    }
	 
	    @ExceptionHandler(BadRequest.class)
	    @ResponseStatus(HttpStatus.BAD_REQUEST)
	    @RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	    public ResponseEntity<ErrorResponse> badRequest(BadRequest ex) {
	       ErrorResponse response =  new ErrorResponse(400, ex.getMessage());
	        log.warn("BadRequest: {}", ex.getMessage());
	        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	    }
	    
	    @ExceptionHandler(InternalServerError.class)
	    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	    @RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	    public ResponseEntity<ErrorResponse> internalServerError(InternalServerError ex) {
	       ErrorResponse response =  new ErrorResponse(500, ex.getMessage());
	        log.error("InternalServerError: {}", ex.getMessage(), ex);
	        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	    
	    @ExceptionHandler(NotFound.class)
	    @ResponseStatus(HttpStatus.NOT_FOUND)
	    @RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	    public ResponseEntity<ErrorResponse> notFound(NotFound ex) {
	       ErrorResponse response =  new ErrorResponse(404, ex.getMessage());
	        log.info("NotFound: {}", ex.getMessage());
	        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
	    }

	    @ExceptionHandler(com.restapp.exception.Unauthorized.class)
	    @ResponseStatus(HttpStatus.UNAUTHORIZED)
	    @RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	    public ResponseEntity<ErrorResponse> unauthorized(com.restapp.exception.Unauthorized ex) {
	        ErrorResponse response = new ErrorResponse(401, ex.getMessage());
	        log.warn("Unauthorized: {}", ex.getMessage());
	        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
	    }

        @ExceptionHandler(BadCredentialsException.class)
        @ResponseStatus(HttpStatus.UNAUTHORIZED)
        @RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        ErrorResponse response = new ErrorResponse(401, "Bad credentials");
        log.warn("Authentication failed: {}", ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

	    @ExceptionHandler(Exception.class)
	    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	    @RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	    public ResponseEntity<ErrorResponse> handleAll(Exception ex) {
	        ErrorResponse response = new ErrorResponse(500, "An unexpected error occurred");
	        log.error("Unhandled exception: {}", ex.getMessage(), ex);
	        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
	    }
}
