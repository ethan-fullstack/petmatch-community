package com.petmatch.community.service.report;

import com.petmatch.community.dto.report.*;
import com.petmatch.community.repository.SupportRequestReportRepository;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SupportRequestReportService {
    public record Report(Page<ReportRow> rows, List<ReportSummary> summary) {}

    private final SupportRequestReportRepository repository;

    public SupportRequestReportService(SupportRequestReportRepository repository) {
        this.repository = repository;
    }

    public Report search(SupportRequestReportFilter filter, int page) {
        filter.validate();
        return new Report(
                repository.search(filter, PageRequest.of(Math.min(100000, Math.max(0, page)), 20)),
                repository.aggregate(filter));
    }
}
