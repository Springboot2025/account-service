package com.legalpro.accountservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lawyer-supplied parameters for generating a Letter of Advice with Claude.
 * Case facts (offence, client questionnaire answers) are loaded server-side
 * from the quote and its client — the frontend only supplies strategy input.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LetterOfAdviceGenerateRequest {

    private String jurisdiction;
    private String strategicGoal;
    private String tone;
    private String templateStyle;
    private String customInstructions;
}
