package com.legalpro.accountservice.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LawyerController {

    @GetMapping("/api/lawyer/hello")
    @PreAuthorize("hasRole('Lawyer')")
    public String helloLawyer() {
        return "Hello Lawyer!";
    }

    // Add Lawyer-specific APIs here
}
