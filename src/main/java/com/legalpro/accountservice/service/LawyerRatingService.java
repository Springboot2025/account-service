package com.legalpro.accountservice.service;

import com.legalpro.accountservice.dto.LawyerRatingDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface LawyerRatingService {

    LawyerRatingDto createOrUpdateRating(LawyerRatingDto dto, UUID clientUuid);

    List<LawyerRatingDto> getRatingsByLawyer(UUID lawyerUuid);

    List<LawyerRatingDto> getRatingsByClient(UUID clientUuid);

    BigDecimal getAverageRatingForLawyer(UUID lawyerUuid);

    void deleteRating(UUID ratingUuid, UUID clientUuid);
}
