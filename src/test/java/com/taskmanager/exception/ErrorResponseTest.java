package com.taskmanager.exception;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ErrorResponseTest {

    // ── Constructors ─────────────────────────────────────────────────────────

    @Test
    void noArgConstructorCreatesInstanceWithTimestamp() {
        ErrorResponse response = new ErrorResponse();

        assertNotNull(response);
        assertNotNull(response.getTimestamp());
        assertNull(response.getCode());
        assertNull(response.getMessage());
        assertNull(response.getDetails());
    }

    @Test
    void twoArgConstructorSetsCodeAndMessage() {
        ErrorResponse response = new ErrorResponse(400, "Bad Request");

        assertEquals(400, response.getCode());
        assertEquals("Bad Request", response.getMessage());
        assertNull(response.getDetails());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void threeArgConstructorSetsCodeMessageAndDetails() {
        List<String> details = List.of("field must not be null", "value is invalid");

        ErrorResponse response = new ErrorResponse(422, "Validation Failed", details);

        assertEquals(422, response.getCode());
        assertEquals("Validation Failed", response.getMessage());
        assertNotNull(response.getDetails());
        assertEquals(2, response.getDetails().size());
        assertTrue(response.getDetails().contains("field must not be null"));
        assertTrue(response.getDetails().contains("value is invalid"));
    }

    // ── Setters / Getters ────────────────────────────────────────────────────

    @Test
    void settersUpdateFieldsCorrectly() {
        ErrorResponse response = new ErrorResponse();
        Instant now = Instant.now();
        List<String> details = List.of("detail1");

        response.setCode(500);
        response.setMessage("Internal Error");
        response.setDetails(details);
        response.setTimestamp(now);

        assertEquals(500, response.getCode());
        assertEquals("Internal Error", response.getMessage());
        assertEquals(details, response.getDetails());
        assertEquals(now, response.getTimestamp());
    }

    @Test
    void timestampIsSetAutomaticallyOnConstruction() {
        Instant before = Instant.now();
        ErrorResponse response = new ErrorResponse();
        Instant after = Instant.now();

        assertNotNull(response.getTimestamp());
        assertFalse(response.getTimestamp().isBefore(before));
        assertFalse(response.getTimestamp().isAfter(after));
    }

    // ── equals / hashCode ────────────────────────────────────────────────────

    @Test
    void twoResponsesWithSameFieldsAreEqual() {
        Instant ts = Instant.parse("2026-01-01T00:00:00Z");
        List<String> details = List.of("err");

        ErrorResponse r1 = new ErrorResponse(400, "msg", details);
        r1.setTimestamp(ts);

        ErrorResponse r2 = new ErrorResponse(400, "msg", details);
        r2.setTimestamp(ts);

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void twoResponsesWithDifferentCodesAreNotEqual() {
        Instant ts = Instant.parse("2026-01-01T00:00:00Z");

        ErrorResponse r1 = new ErrorResponse(400, "msg");
        r1.setTimestamp(ts);

        ErrorResponse r2 = new ErrorResponse(500, "msg");
        r2.setTimestamp(ts);

        assertNotEquals(r1, r2);
    }

    @Test
    void twoResponsesWithDifferentMessagesAreNotEqual() {
        Instant ts = Instant.parse("2026-01-01T00:00:00Z");

        ErrorResponse r1 = new ErrorResponse(400, "message A");
        r1.setTimestamp(ts);

        ErrorResponse r2 = new ErrorResponse(400, "message B");
        r2.setTimestamp(ts);

        assertNotEquals(r1, r2);
    }

    @Test
    void responseIsNotEqualToNull() {
        ErrorResponse response = new ErrorResponse(400, "msg");
        assertNotEquals(null, response);
    }

    @Test
    void responseIsEqualToItself() {
        ErrorResponse response = new ErrorResponse(400, "msg");
        assertSame(response, response);
    }

    // ── Details list variants ────────────────────────────────────────────────

    @Test
    void detailsCanBeSetToEmptyList() {
        ErrorResponse response = new ErrorResponse(400, "Validation Failed", List.of());

        assertNotNull(response.getDetails());
        assertTrue(response.getDetails().isEmpty());
    }

    @Test
    void detailsCanBeSetToNull() {
        ErrorResponse response = new ErrorResponse(400, "msg", null);
        assertNull(response.getDetails());
    }
}


