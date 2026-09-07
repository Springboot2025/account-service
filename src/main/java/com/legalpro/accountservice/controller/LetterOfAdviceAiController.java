package com.legalpro.accountservice.controller;

import com.legalpro.accountservice.dto.LetterOfAdviceContentDto;
import com.legalpro.accountservice.dto.LetterOfAdviceGenerateRequest;
import com.legalpro.accountservice.security.CustomUserDetails;
import com.legalpro.accountservice.service.LetterOfAdviceAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/lawyer/cases/{caseUuid}/letter-of-advice")
@PreAuthorize("hasRole('Lawyer')")
@RequiredArgsConstructor
public class LetterOfAdviceAiController {

    private final LetterOfAdviceAiService letterOfAdviceAiService;

    // Drafting a full letter can take well over a minute; without streaming the
    // browser just sat on one blocking POST with no feedback for its whole
    // duration. Emits "delta" events (raw text chunks, as Claude drafts) so
    // the frontend can show live progress, then one "done" event carrying the
    // final parsed letter, or one "error" event if generation fails.
    private static final long SSE_TIMEOUT_MS = 5 * 60 * 1000L;

    /**
     * Called when the lawyer clicks "Generate Advice" on the Letter of Advice AI tab.
     * Loads the case's offence details and the client's Core/Offence questionnaire
     * answers server-side, streams the draft from Claude, and pushes it to the
     * frontend over SSE for the frontend to render into the letter builder.
     */
    @PostMapping(value = "/generate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generate(
            @PathVariable UUID caseUuid,
            @RequestBody LetterOfAdviceGenerateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID lawyerUuid = userDetails.getUuid();
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        // The Anthropic SDK call blocks the thread that drives it for the
        // whole drafting time, so it must run off the request thread — a
        // virtual thread is cheap enough not to need a dedicated pool for
        // what is a low-volume, per-user-click endpoint.
        Thread.ofVirtual().name("loa-generate-" + caseUuid).start(() -> {
            try {
                LetterOfAdviceContentDto content = letterOfAdviceAiService.generate(lawyerUuid, caseUuid, request,
                        chunk -> {
                            try {
                                emitter.send(SseEmitter.event().name("delta").data(chunk));
                            } catch (IOException | IllegalStateException clientGone) {
                                // Browser navigated away or dropped the connection —
                                // let the generation keep running to completion
                                // server-side rather than aborting the Claude call mid-flight.
                            }
                        });
                emitter.send(SseEmitter.event().name("done").data(content));
                emitter.complete();
            } catch (Exception e) {
                String clientMessage;
                if (e instanceof IllegalArgumentException || e instanceof IllegalStateException) {
                    clientMessage = e.getMessage();
                } else {
                    log.error("Letter of advice generation failed for case {}", caseUuid, e);
                    clientMessage = "Something went wrong while drafting the letter. Please try again.";
                }
                try {
                    emitter.send(SseEmitter.event().name("error").data(clientMessage));
                } catch (IOException | IllegalStateException ignored) {
                    // client already gone; nothing to notify
                }
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}
