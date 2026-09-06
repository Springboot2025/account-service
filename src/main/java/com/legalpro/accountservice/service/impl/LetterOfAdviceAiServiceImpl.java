package com.legalpro.accountservice.service.impl;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
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
import java.util.Map;
import java.util.UUID;

@Service
public class LetterOfAdviceAiServiceImpl implements LetterOfAdviceAiService {

    // Sonnet 4.6 is the right default for this task: strong legal drafting quality
    // at a per-letter cost that stays sane at platform scale. Swap to Opus only if
    // drafting quality on complex matters (e.g. serious indictable offences) proves
    // it's worth the higher cost. Passed as a raw string (the SDK's Model type
    // accepts either its typed constants or a plain string) to avoid depending on
    // an exact Model enum constant name that may not exist in the pinned SDK version.
    private static final String MODEL = "claude-sonnet-4-6";

    private final AnthropicClient anthropicClient;
    private final LegalCaseRepository legalCaseRepository;
    private final QuoteRepository quoteRepository;
    private final ClientAnswerRepository clientAnswerRepository;
    private final ObjectMapper objectMapper;
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
        this.systemPrompt = new String(
                new ClassPathResource("prompts/letter-of-advice-system-prompt.txt")
                        .getInputStream()
                        .readAllBytes(),
                StandardCharsets.UTF_8
        );
    }

    @Override
    public LetterOfAdviceContentDto generate(UUID lawyerUuid, UUID caseUuid, LetterOfAdviceGenerateRequest request) {
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
                .maxTokens(4096L)
                .system(systemPrompt)
                .addUserMessage(userContent)
                .build();

        Message message;
        try {
            message = anthropicClient.messages().create(params);
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "AI letter drafting is not available right now. If this persists, check that "
                            + "Workload Identity Federation is correctly configured for this service.", e);
        }

        return extractLetterContent(message);
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

    private LetterOfAdviceContentDto extractLetterContent(Message message) {
        String rawJson = message.content().stream()
                .flatMap(block -> block.text().stream())
                .map(textBlock -> textBlock.text())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No text content returned by Claude"));

        String cleanedJson = stripMarkdownCodeFence(rawJson);

        try {
            return objectMapper.readValue(cleanedJson, LetterOfAdviceContentDto.class);
        } catch (Exception e) {
            // If this fires often in practice, tighten the system prompt's
            // "return JSON only" instruction, or add a retry with a stricter
            // reminder appended to the user message.
            throw new IllegalStateException("Claude did not return valid letter JSON: " + rawJson, e);
        }
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
