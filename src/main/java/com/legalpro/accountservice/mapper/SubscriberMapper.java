package com.legalpro.accountservice.mapper;

import com.legalpro.accountservice.dto.SubscriberDto;
import com.legalpro.accountservice.entity.Subscriber;
import org.springframework.stereotype.Component;

@Component
public class SubscriberMapper {

    public SubscriberDto toDto(Subscriber entity) {
        if (entity == null) return null;

        return SubscriberDto.builder()
                .id(entity.getId())
                .uuid(entity.getUuid())
                .email(entity.getEmail())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public Subscriber toEntity(SubscriberDto dto) {
        if (dto == null) return null;

        return Subscriber.builder()
                .id(dto.getId())
                .uuid(dto.getUuid())
                .email(dto.getEmail())
                .isActive(dto.getIsActive())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }
}
