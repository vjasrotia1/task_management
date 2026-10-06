package com.varun.taskmgmtapi.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/api/test")
    public String test(){

    return "you are authenticated";
    }
}
/*
Because our security config says:
.anyRequest().authenticated() && /api/auth/** ---> is permitAll()

so this endpoint(/api/test) above is protected

 */

