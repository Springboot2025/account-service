package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.Quote;
import com.legalpro.accountservice.enums.QuoteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuoteRepository extends JpaRepository<Quote, Long> {

    Optional<Quote> findByUuid(UUID uuid);

    List<Quote> findByLawyerUuid(UUID lawyerUuid);

    List<Quote> findByClientUuid(UUID clientUuid);

    List<Quote> findByLawyerUuidAndStatus(UUID lawyerUuid, QuoteStatus status);

    List<Quote> findByClientUuidAndStatus(UUID clientUuid, QuoteStatus status);
}
