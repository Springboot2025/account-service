package com.legalpro.accountservice.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SuperAdminController {

    @GetMapping("/api/superadmin/hello")
    @PreAuthorize("hasRole('SuperAdmin')")
    public String helloSuperAdmin() {
        return "Hello SuperAdmin!";
    }

    // Add SuperAdmin-specific APIs here
}
