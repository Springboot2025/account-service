package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.AccountDto;
import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.service.ContactRequestService;
import com.legalpro.accountservice.service.SuperAdminService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/superadmin")
@PreAuthorize("hasRole('Admin')")
public class SuperAdminController {

    private final SuperAdminService superAdminService;
    private final ContactRequestService contactRequestService;

    public SuperAdminController(SuperAdminService superAdminService, ContactRequestService contactRequestService) {
        this.superAdminService = superAdminService;
        this.contactRequestService = contactRequestService;
    }
    @GetMapping("/hello")
    public ResponseEntity<ApiResponse<String>> helloSuperAdmin() {
        ApiResponse<String> response = ApiResponse.success(
                HttpStatus.OK.value(),
                "SuperAdmin API working",
                "Hello SuperAdmin!"
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<AccountDto>>> getUsersByType(
            @RequestParam(name = "type") String userType) {

        try {
            List<AccountDto> users = superAdminService.getUsersByType(userType);

            return ResponseEntity.ok(
                    ApiResponse.success(HttpStatus.OK.value(), "Fetched users successfully", users)
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ApiResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage())
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Something went wrong")
            );
        }
    }

    @GetMapping("/contacts")
    public ResponseEntity<ApiResponse<?>> listContactRequests() {
        try {
            var list = contactRequestService.listAll();

            return ResponseEntity.ok(
                    ApiResponse.success(
                            HttpStatus.OK.value(),
                            "Fetched contact requests successfully",
                            list
                    )
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(
                            HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Something went wrong while fetching contact requests"
                    )
            );
        }
    }

}
