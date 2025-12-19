package com.legalpro.accountservice.service;

import com.legalpro.accountservice.dto.CategoryWithSubheadingsDto;
import com.legalpro.accountservice.dto.DocumentCategoryDto;
import com.legalpro.accountservice.dto.DocumentTemplateCenterDto;
import com.legalpro.accountservice.dto.LawyerDocumentSubheadingDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface DocumentTemplateCenterService {

    // STEP 6.1
    List<DocumentCategoryDto> getAllCategories();

    // STEP 6.2
    List<CategoryWithSubheadingsDto> getSubheadingsGroupedByCategory(UUID lawyerUuid);

    // STEP 6.3
    LawyerDocumentSubheadingDto createSubheading(
            UUID lawyerUuid,
            Long categoryId,
            String subheadingName
    );

    List<DocumentTemplateCenterDto> uploadDocuments(
            UUID lawyerUuid,
            Long categoryId,
            Long subheadingId,        // nullable
            String newSubheadingName, // nullable
            List<MultipartFile> files
    );

    void deleteDocument(UUID lawyerUuid, UUID documentUuid);

    List<DocumentTemplateCenterDto> getDocumentsByLawyer(UUID lawyerUuid);

    List<LawyerDocumentSubheadingDto> getSubheadingsByLawyer(UUID lawyerUuid);

    List<DocumentTemplateCenterDto> getDocumentsBySubheading(
            UUID lawyerUuid,
            Long subheadingId
    );

}
