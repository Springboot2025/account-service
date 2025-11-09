package com.legalpro.accountservice.mapper;

import com.legalpro.accountservice.dto.ContactRequestDto;
import com.legalpro.accountservice.entity.ContactRequest;
import org.springframework.stereotype.Component;

@Component
public class ContactRequestMapper {

    public ContactRequest toEntity(ContactRequestDto dto) {
        if (dto == null) return null;

        return ContactRequest.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .firmName(dto.getFirmName())
                .message(dto.getMessage())
                .subscribeNewsletter(dto.isSubscribeNewsletter())
                .build();
    }
}
