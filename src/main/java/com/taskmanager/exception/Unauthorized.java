package com.taskmanager.exception;

/**
 * Authentication/authorization failures.
 */
public class Unauthorized extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public Unauthorized(String message) {
        super(message);
    }
}
