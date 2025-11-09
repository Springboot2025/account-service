package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.dto.ContactRequestDto;
import com.legalpro.accountservice.service.ContactRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
public class ContactController {

    private final ContactRequestService contactRequestService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> submit(@RequestBody ContactRequestDto dto) {

        contactRequestService.submit(dto);

        return ResponseEntity.ok(
                ApiResponse.success(200, "Thank you for contacting us. We will get back to you soon.", null)
        );
    }
}
