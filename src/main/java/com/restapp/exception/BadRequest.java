package com.restapp.exception;

public class BadRequest extends Exception{

	private static final long serialVersionUID = -2759657386853888789L;

	public BadRequest(String message) {
		super(message);
	}
}
