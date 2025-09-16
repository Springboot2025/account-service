package com.legalpro.accountservice.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.legalpro.accountservice.dto.AccountDto;
import com.legalpro.accountservice.dto.ApiResponse;
import com.legalpro.accountservice.dto.ClientAnswerRequest;
import com.legalpro.accountservice.entity.Account;
import com.legalpro.accountservice.entity.ClientAnswer;
import com.legalpro.accountservice.entity.QuestionType;
import com.legalpro.accountservice.repository.ClientAnswerRepository;
import com.legalpro.accountservice.security.CustomUserDetails;
import com.legalpro.accountservice.service.AccountService;
import com.legalpro.accountservice.service.ClientAnswerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
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
                            ClientAnswerRepository clientAnswerRepository, ClientAnswerService clientAnswerService) {
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
    public ResponseEntity<ApiResponse<AccountDto>> updateClient(
            @PathVariable UUID uuid,
            @RequestBody AccountDto dto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // Ownership check at controller level
        if (!uuid.equals(userDetails.getUuid())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(HttpStatus.FORBIDDEN.value(), "You can only update your own profile"));
        }

        Account updated = accountService.updateAccount(uuid, dto, userDetails.getUuid());

        AccountDto responseDto = AccountDto.builder()
                .id(updated.getId())
                .uuid(updated.getUuid())
                .firstName(updated.getFirstName())
                .lastName(updated.getLastName())
                .gender(updated.getGender())
                .email(updated.getEmail())
                .mobile(updated.getMobile())
                .address(updated.getAddress())
                .build();

        return ResponseEntity.ok(ApiResponse.success(200, "Updated successfully", responseDto));
    }


    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponse<AccountDto>> getClient(
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
                    AccountDto dto = AccountDto.builder()
                            .id(account.getId())
                            .uuid(account.getUuid())
                            .firstName(account.getFirstName())
                            .lastName(account.getLastName())
                            .gender(account.getGender())
                            .email(account.getEmail())
                            .mobile(account.getMobile())
                            .address(account.getAddress())
                            .build();

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
        // Ownership check
        if (!request.getClientUuid().equals(userDetails.getUuid())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(HttpStatus.FORBIDDEN.value(), "You can only save your own answers"));
        }

        // Convert Map -> JsonNode before saving
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
        // Ownership check
        if (!uuid.equals(userDetails.getUuid())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(HttpStatus.FORBIDDEN.value(), "You can only fetch your own answers"));
        }

        if (questionType != null) {
            // Convert String → Enum
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
                            ResponseEntity.ok((ApiResponse<?>) ApiResponse.success(200, "Answer fetched successfully", answer))
                    )
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), "No answer found for this type")));
        } else {
            // Fetch all answers for client
            var answers = clientAnswerService.findAllByClient(uuid);
            if (answers.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), "No answers found"));
            }
            return ResponseEntity.ok((ApiResponse<?>) ApiResponse.success(200, "Answers fetched successfully", answers));
        }
    }

    @PutMapping("/answers")
    public ResponseEntity<ApiResponse<?>> updateAnswers(
            @RequestBody ClientAnswerRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // Ownership check
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
