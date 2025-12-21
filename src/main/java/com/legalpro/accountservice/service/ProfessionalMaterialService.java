package com.legalpro.accountservice.service;

import com.legalpro.accountservice.dto.ProfessionalMaterialResponseDto;
import com.legalpro.accountservice.dto.ClientProfessionalMaterialsResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
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

    List<ClientProfessionalMaterialsResponseDto> getClientProfessionalMaterials(
            UUID clientUuid,
            UUID caseUuid
    );
}
