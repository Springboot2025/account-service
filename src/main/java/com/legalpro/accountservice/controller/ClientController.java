package com.legalpro.accountservice.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.dto.ClientAnswerRequest;
import com.legalpro.accountservice.dto.ClientDto;
import com.legalpro.accountservice.entity.Account;
import com.legalpro.accountservice.entity.ClientAnswer;
import com.legalpro.accountservice.entity.QuestionType;
import com.legalpro.accountservice.mapper.AccountMapper;
import com.legalpro.accountservice.repository.ClientAnswerRepository;
import com.legalpro.accountservice.security.CustomUserDetails;
import com.legalpro.accountservice.service.AccountService;
import com.legalpro.accountservice.service.ClientAnswerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/client")
@PreAuthorize("hasRole('Client')")
public class ClientController {

    private final AccountService accountService;
    private final ClientAnswerService clientAnswerService;
    private final ClientAnswerRepository clientAnswerRepository;

    public ClientController(AccountService accountService,
                            ClientAnswerRepository clientAnswerRepository,
                            ClientAnswerService clientAnswerService) {
        this.accountService = accountService;
        this.clientAnswerRepository = clientAnswerRepository;
        this.clientAnswerService = clientAnswerService;
    }

    // --- Account Endpoints ---

    @GetMapping("/hello")
    public String helloClient() {
        return "Hello Client!";
    }

    @PatchMapping("/{uuid}")
    public ResponseEntity<ApiResponse<ClientDto>> updateClient(
            @PathVariable UUID uuid,
            @RequestBody ClientDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // Ownership check
        if (!uuid.equals(userDetails.getUuid())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(HttpStatus.FORBIDDEN.value(), "You can only update your own profile"));
        }

        Account updated = accountService.updateAccount(uuid, dto, userDetails.getUuid());

        ClientDto responseDto = AccountMapper.toClientDto(updated);

        return ResponseEntity.ok(ApiResponse.success(200, "Updated successfully", responseDto));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponse<ClientDto>> getClient(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // Ownership check
        if (!uuid.equals(userDetails.getUuid())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(HttpStatus.FORBIDDEN.value(), "You can only access your own profile"));
        }

        return accountService.findByUuid(uuid)
                .map(account -> {
                    ClientDto dto = AccountMapper.toClientDto(account);

                    return ResponseEntity.ok(ApiResponse.success(200, "Client fetched successfully", dto));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), "Client not found")));
    }

    // --- Client Answers Endpoints ---

    @PostMapping("/answers")
    public ResponseEntity<ApiResponse<ClientAnswer>> saveAnswers(
            @RequestBody ClientAnswerRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (!request.getClientUuid().equals(userDetails.getUuid())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(HttpStatus.FORBIDDEN.value(), "You can only save your own answers"));
        }

        JsonNode jsonAnswers = clientAnswerService.convertToJsonNode(request.getAnswers());

        ClientAnswer answer = ClientAnswer.builder()
                .clientUuid(request.getClientUuid())
                .questionType(request.getQuestionType())
                .answers(jsonAnswers)
                .build();

        ClientAnswer saved = clientAnswerService.save(answer);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "Answers saved successfully", saved));
    }

    @GetMapping("/{uuid}/answers")
    public ResponseEntity<ApiResponse<?>> getAnswers(
            @PathVariable UUID uuid,
            @RequestParam(required = false) String questionType,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (!uuid.equals(userDetails.getUuid())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(HttpStatus.FORBIDDEN.value(), "You can only fetch your own answers"));
        }

        if (questionType != null) {
            QuestionType type;
            try {
                type = QuestionType.valueOf(
                        questionType.substring(0, 1).toUpperCase() + questionType.substring(1).toLowerCase()
                );
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "Invalid question type"));
            }

            return clientAnswerService.findByClientAndType(uuid, type)
                    .<ResponseEntity<ApiResponse<?>>>map(answer ->
                            ResponseEntity.ok(ApiResponse.success(200, "Answer fetched successfully", answer))
                    )
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), "No answer found for this type")));
        } else {
            var answers = clientAnswerService.findAllByClient(uuid);
            if (answers.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), "No answers found"));
            }
            return ResponseEntity.ok(ApiResponse.success(200, "Answers fetched successfully", answers));
        }
    }

    @PutMapping("/answers")
    public ResponseEntity<ApiResponse<?>> updateAnswers(
            @RequestBody ClientAnswerRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (!request.getClientUuid().equals(userDetails.getUuid())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(HttpStatus.FORBIDDEN.value(), "You can only update your own answers"));
        }

        Optional<ClientAnswer> updated = clientAnswerService.updateAnswers(
                request.getClientUuid(),
                request.getQuestionType(),
                request.getAnswers()
        );

        return updated
                .<ResponseEntity<ApiResponse<?>>>map(ans ->
                        ResponseEntity.ok(ApiResponse.success(200, "Answers updated successfully", ans))
                )
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), "No record found to update")));
    }
}
