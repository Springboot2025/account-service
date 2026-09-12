package com.legalpro.accountservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.legalpro.accountservice.dto.SupportTicketDetailDto;
import com.legalpro.accountservice.dto.SupportTicketDto;
import com.legalpro.accountservice.dto.TicketAttachmentDto;
import com.legalpro.accountservice.dto.TicketReplyDto;
import com.legalpro.accountservice.dto.admin.AdminTicketListResponse;
import com.legalpro.accountservice.dto.admin.TicketSummaryDto;
import com.legalpro.accountservice.entity.Account;
import com.legalpro.accountservice.entity.SupportTicket;
import com.legalpro.accountservice.entity.SupportTicketAttachment;
import com.legalpro.accountservice.entity.SupportTicketReply;
import com.legalpro.accountservice.entity.TicketCategory;
import com.legalpro.accountservice.entity.TicketPriority;
import com.legalpro.accountservice.entity.TicketStatus;
import com.legalpro.accountservice.repository.AccountRepository;
import com.legalpro.accountservice.repository.SupportTicketAttachmentRepository;
import com.legalpro.accountservice.repository.SupportTicketReplyRepository;
import com.legalpro.accountservice.repository.SupportTicketRepository;
import com.legalpro.accountservice.util.GcsUrlSigner;
import com.legalpro.accountservice.util.UploadValidation;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SupportTicketService {

    private final SupportTicketRepository ticketRepository;
    private final SupportTicketReplyRepository replyRepository;
    private final SupportTicketAttachmentRepository attachmentRepository;
    private final AccountRepository accountRepository;
    private final Storage storage;

    // Same private bucket as other client/lawyer document uploads -- ticket
    // attachments can carry sensitive info, so they're signed on read, never
    // public. See GcsUrlSigner for the signing + caching layer.
    private final String bucketName = "legalpro-client-docs-au";

    public SupportTicketService(
            SupportTicketRepository ticketRepository,
            SupportTicketReplyRepository replyRepository,
            SupportTicketAttachmentRepository attachmentRepository,
            AccountRepository accountRepository
    ) {
        this.ticketRepository = ticketRepository;
        this.replyRepository = replyRepository;
        this.attachmentRepository = attachmentRepository;
        this.accountRepository = accountRepository;
        this.storage = StorageOptions.getDefaultInstance().getService();
    }

    @Transactional
    public SupportTicketDetailDto createTicket(
            UUID creatorUuid,
            String subject,
            String description,
            TicketCategory category,
            TicketPriority priority,
            List<MultipartFile> attachments
    ) throws IOException {
        LocalDateTime now = LocalDateTime.now();
        SupportTicket ticket = SupportTicket.builder()
                .createdByUuid(creatorUuid)
                .subject(subject)
                .description(description)
                .status(TicketStatus.OPEN)
                .priority(priority != null ? priority : TicketPriority.NORMAL)
                .category(category != null ? category : TicketCategory.GENERAL)
                .createdAt(now)
                .updatedAt(now)
                .build();
        ticket = ticketRepository.save(ticket);

        if (attachments != null) {
            for (MultipartFile file : attachments) {
                if (file == null || file.isEmpty()) continue;
                saveAttachment(ticket.getUuid(), null, creatorUuid, file);
            }
        }

        return getTicketDetail(ticket.getUuid(), creatorUuid, false);
    }

    @Transactional
    public SupportTicketDetailDto addReply(
            UUID ticketUuid,
            UUID senderUuid,
            String message,
            List<MultipartFile> attachments,
            boolean isAdmin
    ) throws IOException {
        SupportTicket ticket = ticketRepository.findByUuidAndDeletedAtIsNull(ticketUuid)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));

        if (!isAdmin && !ticket.getCreatedByUuid().equals(senderUuid)) {
            throw new IllegalStateException("You are not allowed to reply to this ticket");
        }

        // A closed ticket is done -- reopen it via updateStatus first if a
        // reply is genuinely needed, rather than allowing replies to pile up
        // on a resolved/archived thread.
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new IllegalStateException("This ticket is closed and no longer accepts replies");
        }

        SupportTicketReply reply = SupportTicketReply.builder()
                .ticketUuid(ticketUuid)
                .senderUuid(senderUuid)
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();
        reply = replyRepository.save(reply);

        if (attachments != null) {
            for (MultipartFile file : attachments) {
                if (file == null || file.isEmpty()) continue;
                saveAttachment(ticketUuid, reply.getUuid(), senderUuid, file);
            }
        }

        // An admin picking up an OPEN ticket moves it forward; a
        // creator replying to a RESOLVED ticket means it wasn't actually
        // resolved for them, so it reopens.
        if (isAdmin && ticket.getStatus() == TicketStatus.OPEN) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        } else if (!isAdmin && ticket.getStatus() == TicketStatus.RESOLVED) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
            ticket.setResolvedAt(null);
        }
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        return getTicketDetail(ticketUuid, senderUuid, isAdmin);
    }

    @Transactional
    public void updateStatus(UUID ticketUuid, TicketStatus newStatus) {
        SupportTicket ticket = ticketRepository.findByUuidAndDeletedAtIsNull(ticketUuid)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));
        ticket.setStatus(newStatus);
        ticket.setUpdatedAt(LocalDateTime.now());
        ticket.setResolvedAt(newStatus == TicketStatus.RESOLVED ? LocalDateTime.now() : null);
        ticketRepository.save(ticket);
    }

    public AdminTicketListResponse getTicketsForAdmin(
            int page, int size, TicketStatus status, TicketPriority priority, TicketCategory category, String search
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        String normalizedSearch = (search == null || search.isBlank()) ? null : search.trim().toLowerCase();

        // Built as a Specification rather than a single JPQL query with
        // "(:param IS NULL OR field = :param)" clauses -- that pattern threw
        // a 500 at runtime because Hibernate couldn't infer a JDBC type for
        // a null parameter compared against an @Enumerated(STRING) column.
        // A Specification only adds a predicate for filters that are
        // actually present, so an absent filter is never bound at all.
        Specification<SupportTicket> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (priority != null) predicates.add(cb.equal(root.get("priority"), priority));
            if (category != null) predicates.add(cb.equal(root.get("category"), category));
            if (normalizedSearch != null) {
                String likePattern = "%" + normalizedSearch + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("subject")), likePattern),
                        cb.like(cb.lower(root.get("description")), likePattern)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<SupportTicket> ticketPage = ticketRepository.findAll(spec, pageable);
        return toListResponse(ticketPage);
    }

    public TicketSummaryDto getTicketsSummaryForAdmin() {
        return TicketSummaryDto.builder()
                .totalTickets(ticketRepository.countTotal())
                .urgentCount(ticketRepository.countUrgentOpen())
                .inProgressCount(ticketRepository.countInProgress())
                .resolvedCount(ticketRepository.countResolved())
                .build();
    }

    public AdminTicketListResponse getMyTickets(UUID uuid, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<SupportTicket> ticketPage =
                ticketRepository.findAllByCreatedByUuidAndDeletedAtIsNullOrderByCreatedAtDesc(uuid, pageable);
        return toListResponse(ticketPage);
    }

    public SupportTicketDetailDto getTicketDetail(UUID ticketUuid, UUID requesterUuid, boolean isAdmin) {
        SupportTicket ticket = ticketRepository.findByUuidAndDeletedAtIsNull(ticketUuid)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));

        if (!isAdmin && !ticket.getCreatedByUuid().equals(requesterUuid)) {
            throw new IllegalStateException("You are not allowed to view this ticket");
        }

        List<SupportTicketReply> replies = replyRepository.findAllByTicketUuidOrderByCreatedAtAsc(ticketUuid);
        List<SupportTicketAttachment> allAttachments = attachmentRepository.findAllByTicketUuid(ticketUuid);

        Set<UUID> peopleUuids = new HashSet<>();
        peopleUuids.add(ticket.getCreatedByUuid());
        replies.forEach(r -> peopleUuids.add(r.getSenderUuid()));
        Map<UUID, Account> accounts = accountRepository.findAllByUuidIn(peopleUuids).stream()
                .collect(Collectors.toMap(Account::getUuid, Function.identity()));

        List<TicketAttachmentDto> ticketAttachments = allAttachments.stream()
                .filter(a -> a.getReplyUuid() == null)
                .map(this::toAttachmentDto)
                .toList();

        Map<UUID, List<TicketAttachmentDto>> attachmentsByReply = allAttachments.stream()
                .filter(a -> a.getReplyUuid() != null)
                .collect(Collectors.groupingBy(
                        SupportTicketAttachment::getReplyUuid,
                        Collectors.mapping(this::toAttachmentDto, Collectors.toList())));

        List<TicketReplyDto> replyDtos = replies.stream().map(r -> {
            Account sender = accounts.get(r.getSenderUuid());
            return TicketReplyDto.builder()
                    .uuid(r.getUuid())
                    .senderUuid(r.getSenderUuid())
                    .senderName(extractFullName(sender))
                    .senderRole(extractRole(sender))
                    .message(r.getMessage())
                    .createdAt(r.getCreatedAt())
                    .attachments(attachmentsByReply.getOrDefault(r.getUuid(), List.of()))
                    .build();
        }).toList();

        Account creator = accounts.get(ticket.getCreatedByUuid());

        return SupportTicketDetailDto.builder()
                .uuid(ticket.getUuid())
                .ticketNumber(formatTicketNumber(ticket))
                .subject(ticket.getSubject())
                .description(ticket.getDescription())
                .status(ticket.getStatus())
                .priority(ticket.getPriority())
                .category(ticket.getCategory())
                .createdByName(extractFullName(creator))
                .createdByRole(extractRole(creator))
                .createdByUuid(ticket.getCreatedByUuid())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .resolvedAt(ticket.getResolvedAt())
                .attachments(ticketAttachments)
                .replies(replyDtos)
                .build();
    }

    private AdminTicketListResponse toListResponse(Page<SupportTicket> ticketPage) {
        List<SupportTicket> tickets = ticketPage.getContent();

        Set<UUID> creatorUuids = tickets.stream().map(SupportTicket::getCreatedByUuid).collect(Collectors.toSet());
        Map<UUID, Account> accounts = accountRepository.findAllByUuidIn(creatorUuids).stream()
                .collect(Collectors.toMap(Account::getUuid, Function.identity()));

        List<UUID> ticketUuids = tickets.stream().map(SupportTicket::getUuid).toList();
        Map<UUID, Long> replyCounts = replyRepository.findAllByTicketUuidIn(ticketUuids).stream()
                .collect(Collectors.groupingBy(SupportTicketReply::getTicketUuid, Collectors.counting()));
        Map<UUID, Long> attachmentCounts = attachmentRepository.findAllByTicketUuidIn(ticketUuids).stream()
                .collect(Collectors.groupingBy(SupportTicketAttachment::getTicketUuid, Collectors.counting()));

        List<SupportTicketDto> dtos = tickets.stream().map(t -> {
            Account creator = accounts.get(t.getCreatedByUuid());
            return SupportTicketDto.builder()
                    .uuid(t.getUuid())
                    .ticketNumber(formatTicketNumber(t))
                    .subject(t.getSubject())
                    .description(t.getDescription())
                    .status(t.getStatus())
                    .priority(t.getPriority())
                    .category(t.getCategory())
                    .createdByName(extractFullName(creator))
                    .createdByRole(extractRole(creator))
                    .createdAt(t.getCreatedAt())
                    .updatedAt(t.getUpdatedAt())
                    .resolvedAt(t.getResolvedAt())
                    .replyCount(replyCounts.getOrDefault(t.getUuid(), 0L).intValue())
                    .attachmentCount(attachmentCounts.getOrDefault(t.getUuid(), 0L).intValue())
                    .build();
        }).toList();

        return AdminTicketListResponse.builder()
                .content(dtos)
                .page(ticketPage.getNumber())
                .size(ticketPage.getSize())
                .totalElements(ticketPage.getTotalElements())
                .totalPages(ticketPage.getTotalPages())
                .build();
    }

    private void saveAttachment(UUID ticketUuid, UUID replyUuid, UUID uploaderUuid, MultipartFile file) throws IOException {
        UploadValidation.validate(file, UploadValidation.DOCUMENT_CONTENT_TYPES);
        String safeFileName = UploadValidation.sanitizeFileName(file.getOriginalFilename());
        String objectName = "support-tickets/" + ticketUuid + "/" + UUID.randomUUID() + "-" + safeFileName;

        BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, objectName)
                .setContentType(file.getContentType())
                .build();
        storage.create(blobInfo, file.getBytes());

        SupportTicketAttachment attachment = SupportTicketAttachment.builder()
                .ticketUuid(ticketUuid)
                .replyUuid(replyUuid)
                .fileName(safeFileName)
                .fileUrl("gs://" + bucketName + "/" + objectName)
                .fileType(file.getContentType())
                .uploadedByUuid(uploaderUuid)
                .createdAt(LocalDateTime.now())
                .build();
        attachmentRepository.save(attachment);
    }

    private TicketAttachmentDto toAttachmentDto(SupportTicketAttachment a) {
        return TicketAttachmentDto.builder()
                .uuid(a.getUuid())
                .fileName(a.getFileName())
                .fileUrl(GcsUrlSigner.sign(a.getFileUrl()))
                .fileType(a.getFileType())
                .build();
    }

    private String formatTicketNumber(SupportTicket ticket) {
        return "TKT-" + ticket.getId();
    }

    private String extractFullName(Account account) {
        if (account == null) {
            return "Unknown";
        }
        JsonNode pd = account.getPersonalDetails();
        if (pd == null) {
            return account.getEmail() != null ? account.getEmail() : "Unknown";
        }
        String firstName = pd.hasNonNull("firstName") ? pd.get("firstName").asText() : "";
        String lastName = pd.hasNonNull("lastName") ? pd.get("lastName").asText() : "";
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isBlank() ? (account.getEmail() != null ? account.getEmail() : "Unknown") : fullName;
    }

    private String extractRole(Account account) {
        if (account == null || account.getRoles() == null || account.getRoles().isEmpty()) {
            return "Unknown";
        }
        String roleName = account.getRoles().iterator().next().getName();
        return roleName.startsWith("ROLE_") ? roleName.substring(5) : roleName;
    }
}
