package com.app.english.exceptions;

public class TaskNotFoundException extends RuntimeException {
    public TaskNotFoundException(String message) { super(message); }
}
