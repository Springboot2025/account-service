package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.dto.LanguageDto;
import com.legalpro.accountservice.service.LanguageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/languages")
@RequiredArgsConstructor
public class LanguageController {
    private final LanguageService languageService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<LanguageDto>>> getLanguages() {
        return ResponseEntity.ok(
                ApiResponse.success(200, "Languages fetched successfully",
                        languageService.getAllLanguages())
        );
    }
}
