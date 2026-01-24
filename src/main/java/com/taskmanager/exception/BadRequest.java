package com.taskmanager.exception;

/**
 * Client-side bad request errors represented as unchecked exception.
 */
public class BadRequest extends RuntimeException {

    private static final long serialVersionUID = -2759657386853888789L;

    public BadRequest(String message) {
        super(message);
    }
}
