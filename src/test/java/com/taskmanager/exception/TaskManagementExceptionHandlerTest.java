package com.taskmanager.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaskManagementExceptionHandlerTest {

    private final TaskManagementExceptionHandler handler = new TaskManagementExceptionHandler();

    // ── MethodArgumentNotValidException ──────────────────────────────────────

    @Test
    void validationExceptionReturnsErrorResponseWithDetails() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult br = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(br);
        when(br.getAllErrors()).thenReturn(List.of(new ObjectError("field", "must not be blank")));

        ResponseEntity<ErrorResponse> resp = handler.processUnmergeException(ex);

        assertNotNull(resp);
        assertEquals(400, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        assertEquals(400, resp.getBody().getCode());
        assertEquals("Validation Failed", resp.getBody().getMessage());
        assertNotNull(resp.getBody().getDetails());
        assertTrue(resp.getBody().getDetails().contains("must not be blank"));
    }

    @Test
    void validationExceptionWithMultipleFieldErrorsReturnsAllDetails() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult br = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(br);
        when(br.getAllErrors()).thenReturn(List.of(
                new ObjectError("title", "must not be blank"),
                new ObjectError("priority", "invalid value")
        ));

        ResponseEntity<ErrorResponse> resp = handler.processUnmergeException(ex);

        assertNotNull(resp.getBody());
        assertEquals(2, resp.getBody().getDetails().size());
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void validationExceptionWithNoErrorsReturnsEmptyDetails() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult br = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(br);
        when(br.getAllErrors()).thenReturn(List.of());

        ResponseEntity<ErrorResponse> resp = handler.processUnmergeException(ex);

        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().getDetails().isEmpty());
    }

    // ── BadRequest ───────────────────────────────────────────────────────────

    @Test
    void badRequestHandledProduces400() {
        ResponseEntity<ErrorResponse> resp = handler.badRequest(new BadRequest("bad input"));

        assertEquals(400, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        assertEquals(400, resp.getBody().getCode());
        assertEquals("bad input", resp.getBody().getMessage());
    }

    @Test
    void badRequestResponseHasCorrectHttpStatus() {
        ResponseEntity<ErrorResponse> resp = handler.badRequest(new BadRequest("duplicate entry"));
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    // ── NotFound ─────────────────────────────────────────────────────────────

    @Test
    void notFoundHandledProduces404() {
        ResponseEntity<ErrorResponse> resp = handler.notFound(new NotFound("not found"));

        assertEquals(404, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        assertEquals(404, resp.getBody().getCode());
        assertEquals("not found", resp.getBody().getMessage());
    }

    @Test
    void notFoundResponseHasCorrectHttpStatus() {
        ResponseEntity<ErrorResponse> resp = handler.notFound(new NotFound("task not found"));
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    // ── InternalServerError ──────────────────────────────────────────────────

    @Test
    void internalServerErrorHandledProduces500() {
        ResponseEntity<ErrorResponse> resp = handler.internalServerError(new InternalServerError("fail"));

        assertEquals(500, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        assertEquals(500, resp.getBody().getCode());
        assertEquals("fail", resp.getBody().getMessage());
    }

    @Test
    void internalServerErrorResponseHasCorrectHttpStatus() {
        ResponseEntity<ErrorResponse> resp = handler.internalServerError(new InternalServerError("db error"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
    }

    // ── Unauthorized ─────────────────────────────────────────────────────────

    @Test
    void unauthorizedHandledProduces401() {
        ResponseEntity<ErrorResponse> resp = handler.unauthorized(new Unauthorized("token expired"));

        assertEquals(401, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        assertEquals(401, resp.getBody().getCode());
        assertEquals("token expired", resp.getBody().getMessage());
    }

    @Test
    void unauthorizedResponseHasCorrectHttpStatus() {
        ResponseEntity<ErrorResponse> resp = handler.unauthorized(new Unauthorized("invalid token"));
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    // ── BadCredentialsException ───────────────────────────────────────────────

    @Test
    void badCredentialsProduces401() {
        ResponseEntity<ErrorResponse> resp = handler.handleBadCredentials(new BadCredentialsException("Bad credentials"));

        assertEquals(401, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        assertEquals(401, resp.getBody().getCode());
        assertEquals("Bad credentials", resp.getBody().getMessage());
    }

    @Test
    void badCredentialsResponseHasCorrectHttpStatus() {
        ResponseEntity<ErrorResponse> resp = handler.handleBadCredentials(new BadCredentialsException("wrong password"));
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    // ── AuthorizationDeniedException ─────────────────────────────────────────

    @Test
    void authorizationDeniedHandledProduces403() {
        AuthorizationDeniedException ex = new AuthorizationDeniedException("Access denied");

        ResponseEntity<ErrorResponse> resp = handler.handleAuthorizationDenied(ex);

        assertEquals(403, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        assertEquals(403, resp.getBody().getCode());
        assertEquals("Access Denied", resp.getBody().getMessage());
    }

    @Test
    void authorizationDeniedResponseHasCorrectHttpStatus() {
        AuthorizationDeniedException ex = new AuthorizationDeniedException("forbidden");
        ResponseEntity<ErrorResponse> resp = handler.handleAuthorizationDenied(ex);
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
    }

    // ── Generic Exception ─────────────────────────────────────────────────────

    @Test
    void genericExceptionHandledProduces500() {
        ResponseEntity<ErrorResponse> resp = handler.handleAll(new RuntimeException("unexpected"));

        assertEquals(500, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        assertEquals(500, resp.getBody().getCode());
        assertEquals("An unexpected error occurred", resp.getBody().getMessage());
    }

    @Test
    void genericExceptionResponseHasCorrectHttpStatus() {
        ResponseEntity<ErrorResponse> resp = handler.handleAll(new Exception("generic error"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
    }

    @Test
    void genericExceptionWithNullMessageHandledGracefully() {
        ResponseEntity<ErrorResponse> resp = handler.handleAll(new Exception((String) null));

        assertNotNull(resp);
        assertEquals(500, resp.getStatusCode().value());
        ErrorResponse body = resp.getBody();
        assertNotNull(body);
        assertEquals("An unexpected error occurred", body.getMessage());
    }
}
