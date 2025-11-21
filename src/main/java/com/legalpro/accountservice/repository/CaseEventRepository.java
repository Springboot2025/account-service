package com.legalpro.accountservice.repository;

import com.legalpro.accountservice.entity.CaseEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CaseEventRepository extends JpaRepository<CaseEvent, Long> {

    List<CaseEvent> findAllByCaseUuidAndDeletedAtIsNullOrderByEventDateDesc(UUID caseUuid);

}
