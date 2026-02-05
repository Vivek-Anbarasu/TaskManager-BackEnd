package com.taskmanager.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class StatusValidationLogicTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void validStatusToDoPassesValidation() {
        TestDTO dto = new TestDTO("To Do");
        Set<ConstraintViolation<TestDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    void validStatusInProgressPassesValidation() {
        TestDTO dto = new TestDTO("In Progress");
        Set<ConstraintViolation<TestDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    void validStatusDonePassesValidation() {
        TestDTO dto = new TestDTO("Done");
        Set<ConstraintViolation<TestDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    void invalidStatusFailsValidation() {
        TestDTO dto = new TestDTO("Invalid Status");
        Set<ConstraintViolation<TestDTO>> violations = validator.validate(dto);
        assertEquals(1, violations.size());
        assertEquals("Status must be either To Do or In Progress or Done",
                     violations.iterator().next().getMessage());
    }

    @Test
    void invalidStatusCompletedFailsValidation() {
        TestDTO dto = new TestDTO("Completed");
        Set<ConstraintViolation<TestDTO>> violations = validator.validate(dto);
        assertEquals(1, violations.size());
    }

    @Test
    void invalidStatusPendingFailsValidation() {
        TestDTO dto = new TestDTO("Pending");
        Set<ConstraintViolation<TestDTO>> violations = validator.validate(dto);
        assertEquals(1, violations.size());
    }

    @Test
    void emptyStringFailsValidation() {
        TestDTO dto = new TestDTO("");
        Set<ConstraintViolation<TestDTO>> violations = validator.validate(dto);
        assertEquals(1, violations.size());
    }

    @Test
    void nullStatusPassesValidation() {
        // Null is considered valid - use @NotNull separately to disallow null
        TestDTO dto = new TestDTO(null);
        Set<ConstraintViolation<TestDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    void statusWithDifferentCasingFailsValidation() {
        TestDTO dto = new TestDTO("to do");  // lowercase
        Set<ConstraintViolation<TestDTO>> violations = validator.validate(dto);
        assertEquals(1, violations.size());
    }

    @Test
    void statusWithExtraSpacesFailsValidation() {
        TestDTO dto = new TestDTO(" To Do ");  // with leading/trailing spaces
        Set<ConstraintViolation<TestDTO>> violations = validator.validate(dto);
        assertEquals(1, violations.size());
    }

    // Test DTO class for testing the validator
    static class TestDTO {
        @StatusValidator
        private String status;

        public TestDTO(String status) {
            this.status = status;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
