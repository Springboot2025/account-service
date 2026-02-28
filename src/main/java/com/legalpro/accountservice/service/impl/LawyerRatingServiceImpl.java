package com.legalpro.accountservice.service.impl;

import com.legalpro.accountservice.dto.LawyerRatingDto;
import com.legalpro.accountservice.entity.LawyerRating;
import com.legalpro.accountservice.mapper.LawyerRatingMapper;
import com.legalpro.accountservice.repository.LawyerRatingRepository;
import com.legalpro.accountservice.service.LawyerRatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class LawyerRatingServiceImpl implements LawyerRatingService {

    private final LawyerRatingRepository lawyerRatingRepository;
    private final LawyerRatingMapper mapper;

    @Override
    public LawyerRatingDto createOrUpdateRating(LawyerRatingDto dto, UUID clientUuid) {
        // Check if this client already rated the lawyer
        LawyerRating existing = lawyerRatingRepository
                .findByLawyerUuidAndClientUuid(dto.getLawyerUuid(), clientUuid)
                .orElse(null);

        if (existing != null) {
            // Update existing rating
            existing.setRating(dto.getRating());
            existing.setReview(dto.getReview());
            existing.setUpdatedAt(LocalDateTime.now());
            lawyerRatingRepository.save(existing);
            return mapper.toDto(existing);
        }

        // New rating
        LawyerRating rating = LawyerRating.builder()
                .uuid(UUID.randomUUID())
                .lawyerUuid(dto.getLawyerUuid())
                .clientUuid(clientUuid)
                .rating(dto.getRating())
                .review(dto.getReview())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        lawyerRatingRepository.save(rating);
        return mapper.toDto(rating);
    }

    @Override
    public List<LawyerRatingDto> getRatingsByLawyer(UUID lawyerUuid) {
        return lawyerRatingRepository.findAllByLawyerUuid(lawyerUuid)
                .stream()
                .filter(r -> r.getDeletedAt() == null)
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<LawyerRatingDto> getRatingsByClient(UUID clientUuid) {
        return lawyerRatingRepository.findAllByClientUuid(clientUuid)
                .stream()
                .filter(r -> r.getDeletedAt() == null)
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public BigDecimal getAverageRatingForLawyer(UUID lawyerUuid) {
        BigDecimal avg = lawyerRatingRepository.findAverageRatingByLawyerUuid(lawyerUuid);
        return avg != null ? avg : BigDecimal.ZERO;
    }

    @Override
    public void deleteRating(UUID ratingUuid, UUID clientUuid) {
        LawyerRating rating = lawyerRatingRepository.findByUuid(ratingUuid)
                .orElseThrow(() -> new IllegalArgumentException("Rating not found"));

        if (!rating.getClientUuid().equals(clientUuid)) {
            throw new SecurityException("You can only delete your own rating");
        }

        rating.setDeletedAt(LocalDateTime.now());
        rating.setUpdatedAt(LocalDateTime.now());
        lawyerRatingRepository.save(rating);
    }
}
