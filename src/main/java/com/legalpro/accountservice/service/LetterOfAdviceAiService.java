package com.legalpro.accountservice.service;

import com.legalpro.accountservice.dto.LetterOfAdviceContentDto;
import com.legalpro.accountservice.dto.LetterOfAdviceGenerateRequest;

import java.util.UUID;

public interface LetterOfAdviceAiService {

    /**
     * Generates the content for a Letter of Advice for the given case, using the
     * originating quote's offence details and the client's Core/Offence
     * questionnaire answers. Blocking call — the underlying Anthropic SDK client
     * is synchronous.
     *
     * @param lawyerUuid the authenticated lawyer — the case must belong to them
     * @param caseUuid   the case (matter) this letter is being drafted for
     * @param request    lawyer-supplied jurisdiction, strategy, and instructions
     */
    LetterOfAdviceContentDto generate(UUID lawyerUuid, UUID caseUuid, LetterOfAdviceGenerateRequest request);
}
