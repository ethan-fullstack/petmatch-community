package com.petmatch.community.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.petmatch.community.dto.report.*;
import com.petmatch.community.repository.SupportRequestReportRepository;
import com.petmatch.community.service.report.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ReportExportServiceTests {
    @Test
    void selectionDelegatesToMatchingExporter() throws Exception {
        var repository = mock(SupportRequestReportRepository.class);
        var pdf = mock(ReportExporter.class);
        var excel = mock(ReportExporter.class);
        when(pdf.format()).thenReturn(ReportExportFormat.PDF);
        when(excel.format()).thenReturn(ReportExportFormat.XLSX);
        when(pdf.mediaType()).thenReturn(MediaType.APPLICATION_PDF);
        when(excel.mediaType()).thenReturn(MediaType.APPLICATION_OCTET_STREAM);
        when(pdf.export(any(), anyList(), anyList())).thenReturn(new byte[]{1});
        when(excel.export(any(), anyList(), anyList())).thenReturn(new byte[]{2});
        var service = new ReportExportService(repository, List.of(pdf, excel));
        var filter = new SupportRequestReportFilter();
        assertArrayEquals(new byte[]{1}, service.export(filter, ReportExportFormat.PDF).content());
        assertArrayEquals(new byte[]{2}, service.export(filter, ReportExportFormat.XLSX).content());
        verify(pdf).export(eq(filter), anyList(), anyList());
        verify(excel).export(eq(filter), anyList(), anyList());
    }

    @Test
    void registryRejectsMissingOrDuplicateStrategies() {
        var repository = mock(SupportRequestReportRepository.class);
        var exporter = mock(ReportExporter.class);
        when(exporter.format()).thenReturn(ReportExportFormat.PDF);
        assertThrows(IllegalStateException.class,
            () -> new ReportExportService(repository, List.of(exporter)));
        assertThrows(IllegalStateException.class,
            () -> new ReportExportService(repository, List.of(exporter, exporter)));
        assertThrows(IllegalArgumentException.class, () -> ReportExportFormat.parse("html"));
    }

    @Test
    void exportsRejectExcessiveResultSetsBeforeRendering() throws Exception {
        var repository = mock(SupportRequestReportRepository.class);
        var pdf = mock(ReportExporter.class);
        var excel = mock(ReportExporter.class);
        when(pdf.format()).thenReturn(ReportExportFormat.PDF);
        when(excel.format()).thenReturn(ReportExportFormat.XLSX);
        when(repository.findForExport(any(), eq(3000))).thenReturn(
            java.util.Collections.nCopies(3001, mock(ReportRow.class)));
        var service = new ReportExportService(repository, List.of(pdf, excel));
        assertThrows(ReportExportLimitExceededException.class,
            () -> service.export(new SupportRequestReportFilter(), ReportExportFormat.PDF));
        verify(excel, never()).export(any(), anyList(), anyList());
    }
}
