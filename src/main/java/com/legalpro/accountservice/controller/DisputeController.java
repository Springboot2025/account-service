package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.dto.DisputeDto;
import com.legalpro.accountservice.entity.DisputeDocument;
import com.legalpro.accountservice.service.DisputeService;
import com.legalpro.accountservice.service.DisputeDocumentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/disputes")
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeService disputeService;
    private final DisputeDocumentService disputeDocumentService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<DisputeDto>> submitDispute(
            @RequestPart("dispute") String disputeJson,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) throws Exception {

        // 1) Parse JSON string into DTO
        DisputeDto dto = objectMapper.readValue(disputeJson, DisputeDto.class);

        // 2) Save dispute record
        DisputeDto savedDispute = disputeService.submitDispute(dto);

        // 3) Upload documents if provided
        if (files != null && !files.isEmpty()) {
            disputeDocumentService.uploadDocuments(savedDispute.getUuid(), files);
        }

        return ResponseEntity.ok(
                ApiResponse.success(200, "Dispute submitted successfully", savedDispute)
        );
    }
}
