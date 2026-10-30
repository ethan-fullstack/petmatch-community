package com.petmatch.community.dto.report;

import com.petmatch.community.model.enums.*;

import java.time.LocalDateTime;

public record ReportRow(
        Long id,
        String title,
        SupportType supportType,
        SupportRequestStatus status,
        String petName,
        LocalDateTime serviceDate) {}
