package com.restapp.exception;

/**
 * Represent server-side errors as an unchecked exception so controllers
 * and services don't need to declare or catch checked exceptions.
 */
public class InternalServerError extends RuntimeException {

    private static final long serialVersionUID = 2237373058671714900L;

    public InternalServerError(String message) {
        super(message);
    }

    public InternalServerError(String message, Throwable cause) {
        super(message, cause);
    }
}
