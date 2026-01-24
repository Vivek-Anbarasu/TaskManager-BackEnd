package com.taskmanager.validation;

import java.lang.annotation.*;

@Documented
@Target({ElementType.FIELD,ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
//@Constraint(validatedBy = StatusValidationLogic.class)
public @interface StatusValidator {

	String message() default "Status must be either To Do or In Progress or Done";
	
}
