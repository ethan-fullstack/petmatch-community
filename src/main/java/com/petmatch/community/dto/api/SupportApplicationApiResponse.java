package com.petmatch.community.dto.api;

import java.time.LocalDateTime;

import com.petmatch.community.model.enums.SupportApplicationStatus;

public record SupportApplicationApiResponse(
    Long id,
    String message,
    LocalDateTime appliedAt,
    SupportApplicationStatus status,
    Long applicantId,
    String applicantName,
    Long supportRequestId,
    String supportRequestTitle
) {
}
