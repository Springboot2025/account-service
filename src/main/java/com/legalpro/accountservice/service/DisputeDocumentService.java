package com.legalpro.accountservice.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface DisputeDocumentService {
    void uploadDocuments(UUID disputeUuid, List<MultipartFile> files) throws IOException;
}
