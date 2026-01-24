package com.restapp.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaskManagementExceptionHandlerTest {

    private final TaskManagementExceptionHandler handler = new TaskManagementExceptionHandler();

    @Test
    void validationExceptionReturnsErrorResponseWithDetails() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult br = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(br);
        when(br.getAllErrors()).thenReturn(List.of(new ObjectError("field", "must not be blank")));

        ResponseEntity<ErrorResponse> resp = handler.processUnmergeException(ex);

        assertNotNull(resp);
        assertEquals(400, resp.getStatusCodeValue());
        assertNotNull(resp.getBody());
        assertEquals(400, resp.getBody().getCode());
        assertEquals("Validation Failed", resp.getBody().getMessage());
        assertNotNull(resp.getBody().getDetails());
        assertTrue(resp.getBody().getDetails().contains("must not be blank"));
    }

    @Test
    void badRequestHandledProduces400() {
        ResponseEntity<ErrorResponse> resp = handler.badRequest(new BadRequest("bad input"));
        assertEquals(400, resp.getStatusCodeValue());
        assertEquals(400, resp.getBody().getCode());
        assertEquals("bad input", resp.getBody().getMessage());
    }

    @Test
    void notFoundHandledProduces404() {
        ResponseEntity<ErrorResponse> resp = handler.notFound(new NotFound("not found"));
        assertEquals(404, resp.getStatusCodeValue());
        assertEquals(404, resp.getBody().getCode());
        assertEquals("not found", resp.getBody().getMessage());
    }

    @Test
    void internalServerErrorHandledProduces500() {
        ResponseEntity<ErrorResponse> resp = handler.internalServerError(new InternalServerError("fail"));
        assertEquals(500, resp.getStatusCodeValue());
        assertEquals(500, resp.getBody().getCode());
        assertEquals("fail", resp.getBody().getMessage());
    }

    @Test
    void badCredentialsProduces401() {
        ResponseEntity<ErrorResponse> resp = handler.handleBadCredentials(new BadCredentialsException("Bad credentials"));
        assertEquals(401, resp.getStatusCodeValue());
        assertEquals(401, resp.getBody().getCode());
        assertEquals("Bad credentials", resp.getBody().getMessage());
    }
}
