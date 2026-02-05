package com.taskmanager.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;

public class StatusValidationLogic implements ConstraintValidator<StatusValidator, String> {

	private static final Set<String> VALID_STATUSES = Set.of("To Do", "In Progress", "Done");

	@Override
	public boolean isValid(String status, ConstraintValidatorContext context) {
		// Null values are considered valid - use @NotNull separately if null is not allowed
		if (status == null) {
			return true;
		}
		return VALID_STATUSES.contains(status);
	}

}
