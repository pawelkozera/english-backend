package com.app.english.exceptions;

public class VocabularyNotFoundException extends RuntimeException {
    public VocabularyNotFoundException(String message) {
        super(message);
    }
}
