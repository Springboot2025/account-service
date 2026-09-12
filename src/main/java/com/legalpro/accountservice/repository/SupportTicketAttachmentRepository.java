package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.SupportTicketAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SupportTicketAttachmentRepository extends JpaRepository<SupportTicketAttachment, Long> {

    List<SupportTicketAttachment> findAllByTicketUuid(UUID ticketUuid);

    List<SupportTicketAttachment> findAllByTicketUuidIn(List<UUID> ticketUuids);
}
