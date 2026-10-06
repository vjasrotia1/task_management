package com.varun.taskmgmtapi.exception;

//if someone sends a request GET/api/tasks/999
//but if task_id 999 doesnot exists we want to throw 404 NOT FOUND error message
//instead of nullpointer exception or generic "Internal server error"

public class ResourceNotFoundException extends RuntimeException{
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
