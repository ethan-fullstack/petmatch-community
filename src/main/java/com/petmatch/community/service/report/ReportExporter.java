package com.petmatch.community.service.report;

import com.petmatch.community.dto.report.*;
import java.io.IOException;
import java.util.List;
import org.springframework.http.MediaType;

public interface ReportExporter {
    ReportExportFormat format();
    MediaType mediaType();
    byte[] export(SupportRequestReportFilter filter, List<ReportRow> rows, List<ReportSummary> summary) throws IOException;
}
