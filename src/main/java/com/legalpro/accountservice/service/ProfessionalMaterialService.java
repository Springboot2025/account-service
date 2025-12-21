package com.legalpro.accountservice.service;

import com.legalpro.accountservice.dto.ProfessionalMaterialResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

public interface ProfessionalMaterialService {

    ProfessionalMaterialResponseDto uploadProfessionalMaterial(
            UUID lawyerUuid,
            UUID caseUuid,
            Long documentCatId,
            String followUp,
            String description,
            MultipartFile file
    ) throws IOException;
}
