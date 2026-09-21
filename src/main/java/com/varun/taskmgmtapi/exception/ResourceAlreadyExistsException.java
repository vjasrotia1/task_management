package com.varun.taskmgmtapi.exception;

//eg: Email already registered is a busines rule violation and not spring/system failure
public class ResourceAlreadyExistsException extends RuntimeException {
    public ResourceAlreadyExistsException(String message) {
        super(message);
    }
}
