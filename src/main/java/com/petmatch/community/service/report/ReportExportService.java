package com.petmatch.community.service.report;

import com.petmatch.community.dto.report.*;
import com.petmatch.community.repository.SupportRequestReportRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReportExportService {
    private static final int MAX_EXPORT_ROWS = 3000;
    public record ExportFile(String filename, MediaType mediaType, byte[] content) {}
    private final SupportRequestReportRepository repository;
    private final Map<ReportExportFormat, ReportExporter> exporters = new EnumMap<>(ReportExportFormat.class);

    public ReportExportService(SupportRequestReportRepository repository, List<ReportExporter> availableExporters) {
        this.repository = repository;
        for (var exporter : availableExporters) {
            if (exporters.putIfAbsent(exporter.format(), exporter) != null) throw new IllegalStateException("Duplicate exporter");
        }
        if (exporters.size() != ReportExportFormat.values().length) throw new IllegalStateException("Missing exporter");
    }

    public ExportFile export(SupportRequestReportFilter filter, ReportExportFormat format) {
        filter.validate();
        var exporter = exporters.get(Objects.requireNonNull(format));
        if (exporter == null) throw new IllegalArgumentException("Unsupported export format");
        var rows = repository.findForExport(filter, MAX_EXPORT_ROWS);
        if (rows.size() > MAX_EXPORT_ROWS) throw new ReportExportLimitExceededException(MAX_EXPORT_ROWS);
        try {
            return new ExportFile("petmatch-report." + format.extension(), exporter.mediaType(),
                exporter.export(filter, rows, repository.aggregate(filter)));
        } catch (IOException error) {
            throw new UncheckedIOException("Unable to generate report", error);
        }
    }
}
