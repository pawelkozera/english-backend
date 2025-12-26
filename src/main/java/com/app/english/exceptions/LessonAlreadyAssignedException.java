package com.app.english.exceptions;

public class LessonAlreadyAssignedException extends RuntimeException {
    public LessonAlreadyAssignedException(String message) {
        super(message);
    }
}

