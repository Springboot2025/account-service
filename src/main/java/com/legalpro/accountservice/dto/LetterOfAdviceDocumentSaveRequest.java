package com.legalpro.accountservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Body for creating or updating the draft content of a Letter of Advice.
 * Content is only editable while the document is still in DRAFT status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LetterOfAdviceDocumentSaveRequest {
    private UUID templateUuid;
    private String title;
    private String content;
}
