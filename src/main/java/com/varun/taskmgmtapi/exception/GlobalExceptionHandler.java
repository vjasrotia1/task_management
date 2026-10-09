package com.varun.taskmgmtapi.exception;

import com.varun.taskmgmtapi.dto.exceptionDto.ErrorResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
/*
HTTP is communication protocol
└── communication rules
    ├── request
    ├── response
    ├── status code
    ├── headers
    └── body
 */
//In a SPRING BOOT REST APPLICATION- java obj-->JSON and JSON--->JAVA OBJ(done by JACKSON)
//Spring Boot automatically configures Jackson for you when you use the web starter.
//since we are using @RESTCONTROLLERADVICE--> Spring treats the returned object as the response body and jackson converts it to JSON


//global exception handling- goal of this stage is to replce ugly spring error responses
//like stack trace, internal server error etc
//with clean API response that frontend/client can understand
//we dont want every controller to have a code like Try,catch-- so global exception handling is a central place to handle exceptions
//flow will be like- controller-->service-->if exception occurs-->redirected to Globalexception Handler class--> clean JSON response
@RestControllerAdvice
//@ControllerAdvice -- means "This class contains exception-handling logic for controllers."
//+
//@ResponseBody -- means --"The value returned by the method should be sent as the HTTP response body."
//Spring then uses its message conversion mechanism to turn it into JSON.
//@RestControllerAdvice-- tells spring that this class will handle exceptions thrown by my controllers
//instead of different controllers handlign their error separately
public class GlobalExceptionHandler {

//spring catches the execption and finds the matching exception handler and this handler then creates the error response and client then receives HTTP 409 say
@ExceptionHandler(ResourceAlreadyExistsException.class)
//@ExceptionHandler(ResourceAlreadyExistsException.class)-- tells spring that wherever "ResourceAlreadyExistsException" occurs, call this method
@ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponseDto handleResourceAlreadyExistsException(ResourceAlreadyExistsException exception) {

        return new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                "conflict",
                exception.getMessage(),null
        );
    }

    //just add-- import org.springframework.web.bind.MethodArgumentNotValidException; at the top
    //purpose of this method--
    //When validation fails, collect all validation errors and send them back to the client in a simple JSON format.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidationErrors(
            MethodArgumentNotValidException ex) {

    //why map?? because, fieldname--> corresponding validation error message
        //eg: errors.put("name", "Name is required");
        Map<String, String> errors = new HashMap<>();

        /*
        other way of writing
        List<FieldError> fieldErrors =
        ex.getBindingResult().getFieldErrors();

for (FieldError error : fieldErrors) {

    String fieldName = error.getField();

    String message = error.getDefaultMessage();

    errors.put(fieldName, message);
}
         */

        //it means--"From the exception, give me the information about the validation that failed."
        ex.getBindingResult()
                //"Give me the validation errors related to fields."
                .getFieldErrors()
                //basically ex.bindingresult().getFieldErrors() means List<FieldError>
                .forEach(error ->
                        errors.put(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );
        return errors;
        //Because this is a REST controller advice, Spring converts the Map into JSON.

        //for(FieldError error : ex.getBindingResult().getFieldErrors()) {}

        /* example:
        FieldError
    field = "name"
    message = "Name is required"

FieldError
    field = "email"
    message = "Invalid email"

FieldError
    field = "password"
    message = "Password must have at least 6 characters"
         */
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponseDto handleResourceNotFoundException(ResourceNotFoundException exception) {
    //;log.warn("Resource not found: {}", ex.getMessage())
    return new ErrorResponseDto(
            LocalDateTime.now(),
            HttpStatus.NOT_FOUND.value(),
            "resourceNotFound",
            exception.getMessage(),null
    );
}

@ExceptionHandler(InvalidCredentialsException.class)
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public ErrorResponseDto handleInvalidCredentialsException(InvalidCredentialsException exception) {
//log.warn("Authentication failed");
    return new ErrorResponseDto(
    LocalDateTime.now(),
            HttpStatus.UNAUTHORIZED.value(), //--->401
            "Unauthorised",
            exception.getMessage(),null
    );

}

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponseDto handleIllegalArgumentException(
            IllegalArgumentException ex) {

       return new ErrorResponseDto(
               LocalDateTime.now(),
               HttpStatus.BAD_REQUEST.value(),
               "Bad Request",
               ex.getMessage(),null
       );
    }
}
