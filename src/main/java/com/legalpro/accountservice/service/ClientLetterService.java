package com.legalpro.accountservice.service;

import com.legalpro.accountservice.dto.ClientLetterDto;

import java.util.List;
import java.util.UUID;

public interface ClientLetterService {

    List<ClientLetterDto> getClientLetters(UUID clientUuid);
}
