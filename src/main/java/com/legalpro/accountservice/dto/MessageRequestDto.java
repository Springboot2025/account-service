package com.legalpro.accountservice.dto;

import lombok.*;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageRequestDto {
    private UUID receiverUuid;
    private String content;
}
