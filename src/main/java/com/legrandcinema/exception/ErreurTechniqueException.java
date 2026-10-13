package com.legrandcinema.exception;

public class ErreurTechniqueException extends RuntimeException {

    public ErreurTechniqueException(String message) {
        super(message);
    }

    public ErreurTechniqueException(String message, Throwable cause) {
        super(message, cause);
    }
}