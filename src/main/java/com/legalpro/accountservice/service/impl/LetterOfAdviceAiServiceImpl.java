package com.legalpro.accountservice.service.impl;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.http.StreamResponse;
import com.anthropic.models.messages.CacheControlEphemeral;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.RawMessageStreamEvent;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.TextBlockParam;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalpro.accountservice.dto.LetterOfAdviceContentDto;
import com.legalpro.accountservice.dto.LetterOfAdviceGenerateRequest;
import com.legalpro.accountservice.entity.ClientAnswer;
import com.legalpro.accountservice.entity.LegalCase;
import com.legalpro.accountservice.entity.QuestionType;
import com.legalpro.accountservice.entity.Quote;
import com.legalpro.accountservice.repository.ClientAnswerRepository;
import com.legalpro.accountservice.repository.LegalCaseRepository;
import com.legalpro.accountservice.repository.QuoteRepository;
import com.legalpro.accountservice.service.LetterOfAdviceAiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Service
public class LetterOfAdviceAiServiceImpl implements LetterOfAdviceAiService {

    // Sonnet 5 is the right default for this task: strong legal drafting quality
    // at a per-letter cost that stays sane at platform scale ($2/$10 per
    // million input/output tokens -- cheaper than 4.6's $3/$15, and it's the
    // newer model). Swap to Opus only if drafting quality on complex matters
    // (e.g. serious indictable offences) proves it's worth the higher cost.
    // Passed as a raw string (the SDK's Model type accepts either its typed
    // constants or a plain string) to avoid depending on an exact Model enum
    // constant name that may not exist in the pinned SDK version.
    private static final String MODEL = "claude-sonnet-5";

    private final AnthropicClient anthropicClient;
    private final LegalCaseRepository legalCaseRepository;
    private final QuoteRepository quoteRepository;
    private final ClientAnswerRepository clientAnswerRepository;
    private final ObjectMapper objectMapper;
    private final ObjectMapper aiResponseObjectMapper;
    private final String systemPrompt;

    @Autowired
    public LetterOfAdviceAiServiceImpl(
            AnthropicClient anthropicClient,
            LegalCaseRepository legalCaseRepository,
            QuoteRepository quoteRepository,
            ClientAnswerRepository clientAnswerRepository,
            ObjectMapper objectMapper
    ) throws IOException {
        this.anthropicClient = anthropicClient;
        this.legalCaseRepository = legalCaseRepository;
        this.quoteRepository = quoteRepository;
        this.clientAnswerRepository = clientAnswerRepository;
        this.objectMapper = objectMapper;
        this.aiResponseObjectMapper = objectMapper.copy()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true);
        this.systemPrompt = new String(
                new ClassPathResource("prompts/letter-of-advice-system-prompt.txt")
                        .getInputStream()
                        .readAllBytes(),
                StandardCharsets.UTF_8
        );
    }

    @Override
    public LetterOfAdviceContentDto generate(UUID lawyerUuid, UUID caseUuid, LetterOfAdviceGenerateRequest request, Consumer<String> onTextDelta) {
        LegalCase legalCase = legalCaseRepository.findByUuid(caseUuid)
                .orElseThrow(() -> new RuntimeException("Case not found"));

        if (!legalCase.getLawyerUuid().equals(lawyerUuid)) {
            throw new RuntimeException("Access denied: not your case");
        }

        // The offence list lives on the originating Quote, not the Case itself.
        Quote quote = quoteRepository.findByUuid(legalCase.getQuoteUuid()).orElse(null);

        Map<String, Object> caseData = buildCaseData(legalCase, quote, request);

        String userContent;
        try {
            userContent = objectMapper.writeValueAsString(caseData);
        } catch (Exception e) {
            throw new IllegalArgumentException("Case data could not be serialised", e);
        }

        MessageCreateParams params = MessageCreateParams.builder()
                .model(MODEL)
                // A full structured letter can legitimately run long (many facts,
                // a long narrative, several resolution options). 4096 was too
                // tight and was silently truncating some responses mid-JSON —
                // Claude would return cut-off, unparseable content for longer
                // matters while shorter ones happened to fit and succeeded.
                // 8192 was still hit in production on a "comprehensive"-style
                // letter with a long optionsForResolution array. Raised to
                // 16000 (Anthropic's recommended non-streaming default) to
                // push the ceiling well above realistic letter sizes; the
                // stopReason==MAX_TOKENS guard below stays as a backstop.
                .maxTokens(16000L)
                // The system prompt is identical on every call — mark it cached
                // so repeat generations only pay full input price for the
                // case-specific data, not the (much larger) instructions.
                .system(MessageCreateParams.System.ofTextBlockParams(List.of(
                        TextBlockParam.builder()
                                .text(systemPrompt)
                                .cacheControl(CacheControlEphemeral.builder().build())
                                .build())))
                .addUserMessage(userContent)
                .build();

        // Streamed rather than a single blocking create() call so the caller
        // (the controller, over SSE) can forward live progress to the browser
        // instead of the request just sitting there for the full ~1-2 minute
        // drafting time with no feedback. We still assemble the full text
        // ourselves and parse it only once the stream ends -- the letter is
        // structured JSON, not something that can be usefully rendered
        // half-formed.
        StringBuilder rawText = new StringBuilder();
        AtomicReference<StopReason> finalStopReason = new AtomicReference<>();
        try (StreamResponse<RawMessageStreamEvent> streamResponse = anthropicClient.messages().createStreaming(params)) {
            streamResponse.stream().forEach(event -> {
                event.contentBlockDelta()
                        .flatMap(delta -> delta.delta().text())
                        .ifPresent(textDelta -> {
                            rawText.append(textDelta.text());
                            onTextDelta.accept(textDelta.text());
                        });
                event.messageDelta()
                        .flatMap(delta -> delta.delta().stopReason())
                        .ifPresent(finalStopReason::set);
            });
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "AI letter drafting is not available right now. If this persists, check that "
                            + "Workload Identity Federation is correctly configured for this service.", e);
        }

        // StopReason is not a true Java enum (it's a Kotlin-generated wrapper
        // class with a custom equals()) -- "==" compares object identity and
        // is essentially always false here, silently defeating this entire
        // check since a freshly-deserialized StopReason is never the same
        // instance as the StopReason.MAX_TOKENS constant. Every truncated
        // response has been falling through to the generic JSON-parse error
        // below instead of this clear message. Must use .equals().
        if (finalStopReason.get() != null && finalStopReason.get().equals(StopReason.MAX_TOKENS)) {
            throw new IllegalStateException(
                    "Claude's response was cut off before it finished drafting the letter (too long for "
                            + "the current output limit). Try again, or shorten the requested scope.");
        }

        return extractLetterContent(rawText.toString());
    }

    private Map<String, Object> buildCaseData(LegalCase legalCase, Quote quote, LetterOfAdviceGenerateRequest request) {
        String matterTitle = legalCase.getListing() != null ? legalCase.getListing() : legalCase.getName();

        Map<String, Object> caseData = new LinkedHashMap<>();
        caseData.put("caseReference", legalCase.getUuid());
        caseData.put("caseNumber", legalCase.getCaseNumber());
        caseData.put("matterTitle", matterTitle);
        caseData.put("offenceList", quote != null ? quote.getOffenceList() : null);
        caseData.put("jurisdiction", request.getJurisdiction());
        caseData.put("strategicGoal", request.getStrategicGoal());
        caseData.put("tone", request.getTone());
        caseData.put("templateStyle", request.getTemplateStyle());
        caseData.put("customInstructions", request.getCustomInstructions());
        caseData.put("coreQuestionnaireAnswers", findAnswers(legalCase.getClientUuid(), QuestionType.Core));
        caseData.put("offenceQuestionnaireAnswers", findAnswers(legalCase.getClientUuid(), QuestionType.Offence));
        return caseData;
    }

    private JsonNode findAnswers(UUID clientUuid, QuestionType questionType) {
        return clientAnswerRepository
                .findByClientUuidAndQuestionTypeAndDeletedAtIsNull(clientUuid, questionType)
                .map(ClientAnswer::getAnswers)
                .orElse(null);
    }

    private LetterOfAdviceContentDto extractLetterContent(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            throw new IllegalStateException("No text content returned by Claude");
        }

        String cleanedJson = extractJsonObject(stripMarkdownCodeFence(rawJson));

        try {
            return aiResponseObjectMapper.readValue(cleanedJson, LetterOfAdviceContentDto.class);
        } catch (Exception e) {
            // If this fires often in practice, tighten the system prompt's
            // "return JSON only" instruction, or add a retry with a stricter
            // reminder appended to the user message.
            throw new IllegalStateException("Claude did not return valid letter JSON: " + e.getMessage(), e);
        }
    }

    private String extractJsonObject(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed;
        }

        int openingBraceIndex = trimmed.indexOf('{');
        int closingBraceIndex = trimmed.lastIndexOf('}');
        if (openingBraceIndex == -1 || closingBraceIndex <= openingBraceIndex) {
            return trimmed;
        }

        return trimmed.substring(openingBraceIndex, closingBraceIndex + 1).trim();
    }

    /**
     * Claude sometimes wraps its JSON response in a markdown code fence
     * (```json ... ```) despite the system prompt asking for raw JSON only.
     * Strip that fence if present; leave the text untouched otherwise.
     */
    private String stripMarkdownCodeFence(String text) {
        String trimmed = text.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }

        int firstNewline = trimmed.indexOf('\n');
        if (firstNewline == -1) {
            return trimmed;
        }

        String withoutOpeningFence = trimmed.substring(firstNewline + 1);
        int closingFenceIndex = withoutOpeningFence.lastIndexOf("```");
        if (closingFenceIndex == -1) {
            return withoutOpeningFence.trim();
        }

        return withoutOpeningFence.substring(0, closingFenceIndex).trim();
    }
}
