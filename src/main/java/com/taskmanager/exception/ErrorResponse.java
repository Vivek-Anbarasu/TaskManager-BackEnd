package com.taskmanager.exception;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Setter @Getter @EqualsAndHashCode
public class ErrorResponse {
    private Instant timestamp = Instant.now();
    private Integer code;
    private String message;
    private List<String> details;

    public ErrorResponse() {}

    public ErrorResponse(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public ErrorResponse(Integer code, String message, List<String> details) {
        this.code = code;
        this.message = message;
        this.details = details;
    }
}
