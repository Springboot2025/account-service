package com.legalpro.accountservice.service;

import com.legalpro.accountservice.dto.ContactRequestDto;

import java.util.List;

public interface ContactRequestService {
    void submit(ContactRequestDto dto);
    List<ContactRequestDto> listAll();
}
