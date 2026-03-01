package com.taskmanager.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CustomExceptionsTest {

    // ── BadRequest ────────────────────────────────────────────────────────────

    @Test
    void badRequestIsRuntimeException() {
        BadRequest ex = new BadRequest("duplicate entry");
        assertInstanceOf(RuntimeException.class, ex);
    }

    @Test
    void badRequestPreservesMessage() {
        BadRequest ex = new BadRequest("duplicate entry");
        assertEquals("duplicate entry", ex.getMessage());
    }

    @Test
    void badRequestCanBeThrown() {
        assertThrows(BadRequest.class, () -> { throw new BadRequest("thrown"); });
    }

    // ── NotFound ─────────────────────────────────────────────────────────────

    @Test
    void notFoundIsRuntimeException() {
        NotFound ex = new NotFound("task not found");
        assertInstanceOf(RuntimeException.class, ex);
    }

    @Test
    void notFoundPreservesMessage() {
        NotFound ex = new NotFound("task not found");
        assertEquals("task not found", ex.getMessage());
    }

    @Test
    void notFoundCanBeThrown() {
        assertThrows(NotFound.class, () -> { throw new NotFound("thrown"); });
    }

    // ── Unauthorized ──────────────────────────────────────────────────────────

    @Test
    void unauthorizedIsRuntimeException() {
        Unauthorized ex = new Unauthorized("token expired");
        assertInstanceOf(RuntimeException.class, ex);
    }

    @Test
    void unauthorizedPreservesMessage() {
        Unauthorized ex = new Unauthorized("token expired");
        assertEquals("token expired", ex.getMessage());
    }

    @Test
    void unauthorizedCanBeThrown() {
        assertThrows(Unauthorized.class, () -> { throw new Unauthorized("thrown"); });
    }

    // ── InternalServerError ───────────────────────────────────────────────────

    @Test
    void internalServerErrorIsRuntimeException() {
        InternalServerError ex = new InternalServerError("db failure");
        assertInstanceOf(RuntimeException.class, ex);
    }

    @Test
    void internalServerErrorMessageConstructorPreservesMessage() {
        InternalServerError ex = new InternalServerError("db failure");
        assertEquals("db failure", ex.getMessage());
    }

    @Test
    void internalServerErrorMessageAndCauseConstructorPreservesBoth() {
        Throwable cause = new IllegalStateException("root cause");
        InternalServerError ex = new InternalServerError("wrapped error", cause);

        assertEquals("wrapped error", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }

    @Test
    void internalServerErrorCauseIsNullWhenNotProvided() {
        InternalServerError ex = new InternalServerError("no cause");
        assertNull(ex.getCause());
    }

    @Test
    void internalServerErrorCanBeThrown() {
        assertThrows(InternalServerError.class, () -> { throw new InternalServerError("thrown"); });
    }

    @Test
    void internalServerErrorWithCauseCanBeThrown() {
        Throwable cause = new RuntimeException("cause");
        assertThrows(InternalServerError.class, () -> { throw new InternalServerError("with cause", cause); });
    }
}

