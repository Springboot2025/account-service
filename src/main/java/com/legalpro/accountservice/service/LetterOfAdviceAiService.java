package com.legalpro.accountservice.service;

import com.legalpro.accountservice.dto.LetterOfAdviceContentDto;
import com.legalpro.accountservice.dto.LetterOfAdviceGenerateRequest;

import java.util.UUID;
import java.util.function.Consumer;

public interface LetterOfAdviceAiService {

    /**
     * Generates the content for a Letter of Advice for the given case, using the
     * originating quote's offence details and the client's Core/Offence
     * questionnaire answers. Blocking call — streams the response from Claude
     * internally (rather than waiting for one large non-streaming response) and
     * invokes {@code onTextDelta} with each raw text chunk as it arrives, so a
     * caller can forward live progress (e.g. over SSE) while still getting back
     * the final parsed result once the whole letter has been drafted.
     *
     * @param lawyerUuid  the authenticated lawyer — the case must belong to them
     * @param caseUuid    the case (matter) this letter is being drafted for
     * @param request     lawyer-supplied jurisdiction, strategy, and instructions
     * @param onTextDelta called on the invoking thread with each raw text chunk as Claude streams it
     */
    LetterOfAdviceContentDto generate(UUID lawyerUuid, UUID caseUuid, LetterOfAdviceGenerateRequest request, Consumer<String> onTextDelta);
}
