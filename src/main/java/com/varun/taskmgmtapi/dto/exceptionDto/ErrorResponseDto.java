package com.varun.taskmgmtapi.dto.exceptionDto;
//we will create obj of ths class using allargs constructor and then we might probably need values of attributes for this obj,
//which we will get using getters
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@AllArgsConstructor
public class ErrorResponseDto {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    //private Map<String, String> errors;
    //with this (commented out) attribute above, Now we can represent both: normal errors(like resource already exists) and validation errors
}
