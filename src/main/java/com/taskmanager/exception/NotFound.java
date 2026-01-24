package com.taskmanager.exception;

/**
 * Resource not found situations as unchecked exception.
 */
public class NotFound extends RuntimeException {

    private static final long serialVersionUID = -2716780981128579484L;

    public NotFound(String message) {
        super(message);
    }
}
