package com.petmatch.community.dto.api;

import java.time.LocalDateTime;

import com.petmatch.community.model.enums.SupportRequestStatus;
import com.petmatch.community.model.enums.SupportType;

public record SupportRequestApiResponse(
    Long id,
    String title,
    String description,
    SupportType supportType,
    LocalDateTime createdAt,
    LocalDateTime serviceDate,
    SupportRequestStatus status,
    Long petId,
    String petName,
    Long ownerId,
    String ownerName
) {
}
