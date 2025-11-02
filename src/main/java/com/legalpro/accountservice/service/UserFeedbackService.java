package com.legalpro.accountservice.service;

import com.legalpro.accountservice.dto.UserFeedbackDto;
import java.util.List;
import java.util.UUID;

public interface UserFeedbackService {

    UserFeedbackDto submitFeedback(UUID userUuid, UserFeedbackDto dto);

    List<UserFeedbackDto> getMyFeedback(UUID userUuid);

    List<UserFeedbackDto> getPublicFeedback();
}
