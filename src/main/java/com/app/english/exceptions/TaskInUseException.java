package com.app.english.exceptions;

public class TaskInUseException extends RuntimeException {
    public TaskInUseException(String message) {
        super(message);
    }
}
