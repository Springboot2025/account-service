package com.legalpro.accountservice.service.impl;

import com.legalpro.accountservice.dto.DisputeDto;
import com.legalpro.accountservice.entity.Dispute;
import com.legalpro.accountservice.mapper.DisputeMapper;
import com.legalpro.accountservice.repository.DisputeRepository;
import com.legalpro.accountservice.service.DisputeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DisputeServiceImpl implements DisputeService {

    private final DisputeRepository disputeRepository;
    private final DisputeMapper disputeMapper;

    @Override
    public DisputeDto submitDispute(DisputeDto dto) {
        Dispute entity = disputeMapper.toEntity(dto);
        Dispute saved = disputeRepository.save(entity);

        log.info("📝 Dispute submitted: {} ({})", saved.getUuid(), saved.getEmail());
        return disputeMapper.toDto(saved);
    }

    @Override
    public List<DisputeDto> getAll() {
        return disputeRepository.findAll()
                .stream()
                .map(disputeMapper::toDto)
                .toList();
    }

    @Override
    public DisputeDto getOne(UUID disputeUuid) {
        return disputeRepository.findByUuid(disputeUuid)
                .map(disputeMapper::toDto)
                .orElseThrow(() -> new RuntimeException("Dispute not found"));
    }

    @Override
    public List<DisputeDto> getAllWithDocumentCount() {

        var disputes = disputeRepository.findAll()
                .stream()
                .map(disputeMapper::toDto)
                .toList();

        // For each dispute, count docs (could optimize later if needed)
        for (DisputeDto dto : disputes) {
            int docCount = disputeDocumentRepository.findAllByDisputeUuid(dto.getUuid()).size();
            dto.setDocumentCount(docCount); // we will add this field below
        }

        return disputes;
    }

}
