package com.legalpro.accountservice.service.impl;

import com.legalpro.accountservice.dto.UserFeedbackDto;
import com.legalpro.accountservice.entity.UserFeedback;
import com.legalpro.accountservice.mapper.UserFeedbackMapper;
import com.legalpro.accountservice.repository.UserFeedbackRepository;
import com.legalpro.accountservice.service.UserFeedbackService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserFeedbackServiceImpl implements UserFeedbackService {

    private final UserFeedbackRepository repository;
    private final UserFeedbackMapper mapper;

    @Override
    public UserFeedbackDto submitFeedback(UUID userUuid, UserFeedbackDto dto) {
        log.info("Submitting feedback from user: {}", userUuid);

        UserFeedback feedback = UserFeedback.builder()
                .userUuid(userUuid)
                .rating(dto.getRating())
                .review(dto.getReview())
                .isPublic(dto.getIsPublic() != null ? dto.getIsPublic() : true)
                .build();

        UserFeedback saved = repository.save(feedback);
        return mapper.toDto(saved);
    }

    @Override
    public List<UserFeedbackDto> getMyFeedback(UUID userUuid) {
        return repository.findByUserUuid(userUuid)
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserFeedbackDto> getPublicFeedback() {
        return repository.findAllPublic()
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }
}
