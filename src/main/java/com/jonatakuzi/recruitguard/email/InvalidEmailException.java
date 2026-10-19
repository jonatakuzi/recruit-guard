package com.jonatakuzi.recruitguard.email;

/** Thrown when pasted text can't be read as an email. Becomes a 400 response. */
public class InvalidEmailException extends RuntimeException {

    public InvalidEmailException(String message) {
        super(message);
    }
}
