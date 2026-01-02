package com.restapp.exception;

public class InternalServerError extends Exception{


	private static final long serialVersionUID = 2237373058671714900L;

	public InternalServerError(String message) {
		super(message);
	}
}
