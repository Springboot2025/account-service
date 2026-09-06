package com.legalpro.accountservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Structured Letter of Advice content returned by the Claude AI drafting service.
 * Field shape mirrors the JSON schema enforced in
 * resources/prompts/letter-of-advice-system-prompt.txt — keep both in sync.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LetterOfAdviceContentDto {

    private String introduction;
    private String ourEngagement;
    private SummaryOfMatter summaryOfMatter;
    private String relevantLaw;
    private CriticalIssue criticalIssue;
    private List<String> personalCircumstances;
    private List<ResolutionOption> optionsForResolution;
    private String employmentImpact;
    private String mentalHealthConsiderations;
    private String interpreterArrangements;
    private List<String> nextSteps;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class FactRow {
        private String label;
        private String value;
        private boolean critical;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SummaryOfMatter {
        private List<FactRow> facts;
        private String narrative;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CriticalIssue {
        private String heading;
        private List<String> implications;
        private String closingInstruction;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ResolutionOption {
        private String heading;
        private String body;
        private List<String> contestedHearingSteps;
    }
}
