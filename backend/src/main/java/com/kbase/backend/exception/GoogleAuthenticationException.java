package com.kbase.backend.exception;

public class GoogleAuthenticationException extends RuntimeException {
    public GoogleAuthenticationException() {
        super("Your Google account could not be verified");
    }
}
