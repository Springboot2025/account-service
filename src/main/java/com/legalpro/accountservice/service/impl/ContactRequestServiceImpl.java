package com.legalpro.accountservice.service.impl;

import com.legalpro.accountservice.dto.ContactRequestDto;
import com.legalpro.accountservice.entity.ContactRequest;
import com.legalpro.accountservice.mapper.ContactRequestMapper;
import com.legalpro.accountservice.repository.ContactRequestRepository;
import com.legalpro.accountservice.service.ContactRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactRequestServiceImpl implements ContactRequestService {

    private final ContactRequestRepository contactRequestRepository;
    private final ContactRequestMapper contactRequestMapper;

    @Override
    public void submit(ContactRequestDto dto) {
        ContactRequest entity = contactRequestMapper.toEntity(dto);
        contactRequestRepository.save(entity);
        log.info("📩 Contact Request submitted from: {} <{}>", dto.getFirstName(), dto.getEmail());
    }

    @Override
    public List<ContactRequestDto> listAll() {
        return contactRequestRepository.findAll()
                .stream()
                .map(contactRequestMapper::toDto)
                .toList();
    }

}
