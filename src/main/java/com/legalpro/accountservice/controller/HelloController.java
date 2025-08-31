package com.legalpro.accountservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/api/0/hello")
    public String hello() {
        return "Hello from Account Service!";
    }
}
