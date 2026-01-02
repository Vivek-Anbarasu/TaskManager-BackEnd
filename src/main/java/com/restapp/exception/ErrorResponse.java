package com.restapp.exception;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Setter @Getter @EqualsAndHashCode
public class ErrorResponse {
Integer code;
String message;
}
