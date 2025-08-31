package com.legalpro.accountservice.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ClientController {

    @GetMapping("/api/client/hello")
    @PreAuthorize("hasRole('Client')")
    public String helloClient() {
        return "Hello Client!";
    }

    // Add Client-specific APIs here
}
