package com.legalpro.accountservice.service;

import com.legalpro.accountservice.dto.ContactRequestDto;

public interface ContactRequestService {
    void submit(ContactRequestDto dto);
}
