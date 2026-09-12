package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.SupportTicketReply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SupportTicketReplyRepository extends JpaRepository<SupportTicketReply, Long> {

    List<SupportTicketReply> findAllByTicketUuidOrderByCreatedAtAsc(UUID ticketUuid);

    List<SupportTicketReply> findAllByTicketUuidIn(List<UUID> ticketUuids);
}
