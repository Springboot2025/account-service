package com.legalpro.accountservice.service;

import com.legalpro.accountservice.dto.NotificationLogDto;
import java.util.List;
import java.util.UUID;

public interface NotificationLogService {
    void saveLog(NotificationLogDto logDto);
    List<NotificationLogDto> getLogsByUser(UUID userUuid);
    void markAsRead(Long id);
    void markAllAsRead(UUID userUuid);

}