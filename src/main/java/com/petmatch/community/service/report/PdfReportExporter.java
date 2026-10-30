package com.petmatch.community.service.report;

import com.petmatch.community.dto.report.*;
import java.io.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class PdfReportExporter implements ReportExporter {
    private static final PDType1Font FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final float[] POSITIONS = {32, 70, 328, 437, 536, 668};
    private static final float[] WIDTHS = {32, 248, 100, 90, 124, 130};
    private static final String[] HEADERS = {"ID", "Title", "Type", "Status", "Pet", "Service date"};

    @Override public ReportExportFormat format() { return ReportExportFormat.PDF; }
    @Override public MediaType mediaType() { return MediaType.APPLICATION_PDF; }

    @Override
    public byte[] export(SupportRequestReportFilter filter, List<ReportRow> rows, List<ReportSummary> summary) throws IOException {
        try (var document = new PDDocument(); var output = new ByteArrayOutputStream()) {
            PDPageContentStream content = null;
            float y = 0;
            int page = 0;
            try {
                for (var record : rows) {
                    if (content == null || y < 50) {
                        if (content != null) content.close();
                        content = newPage(document, filter, rows.size(), ++page);
                        y = 464;
                    }
                    var values = new String[]{String.valueOf(record.id()), record.title(), record.supportType().name(),
                        record.status().name(), record.petName(), DATE_FORMAT.format(record.serviceDate())};
                    for (int i = 0; i < values.length; i++)
                        write(content, FONT, 8, POSITIONS[i], y, values[i], WIDTHS[i]);
                    y -= 15;
                }
                if (content == null || y < 115) {
                    if (content != null) content.close();
                    content = newPage(document, filter, rows.size(), ++page);
                    y = 462;
                }
                y -= 18;
                write(content, BOLD, 11, 32, y, "Grouped summary: " + filter.getGrouping().name(), 760);
                y -= 20;
                if (summary.isEmpty()) write(content, FONT, 9, 32, y, "No matching requests", 760);
                for (var group : summary) {
                    if (y < 46) {
                        content.close();
                        content = newPage(document, filter, rows.size(), ++page);
                        y = 464;
                    }
                    write(content, FONT, 9, 32, y, group.label() + ": " + group.total(), 760);
                    y -= 16;
                }
            } finally {
                if (content != null) content.close();
            }
            document.save(output);
            return output.toByteArray();
        }
    }

    private PDPageContentStream newPage(PDDocument document, SupportRequestReportFilter filter, int total, int page) throws IOException {
        var pdfPage = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
        document.addPage(pdfPage);
        var content = new PDPageContentStream(document, pdfPage);
        write(content, BOLD, 15, 32, 555, "PetMatch Community - Support requests", 760);
        write(content, FONT, 9, 32, 536, "Filtered total: " + total + " | Page: " + page, 760);
        var criteria = "Status: " + (filter.getStatus() == null ? "All" : filter.getStatus().name())
            + " | Type: " + (filter.getSupportType() == null ? "All" : filter.getSupportType().name())
            + " | From: " + (filter.getFromDate() == null ? "All" : filter.getFromDate())
            + " | To: " + (filter.getToDate() == null ? "All" : filter.getToDate());
        write(content, FONT, 8, 32, 518, criteria, 760);
        content.moveTo(32, 503);
        content.lineTo(807, 503);
        content.stroke();
        for (int i = 0; i < HEADERS.length; i++) write(content, BOLD, 8, POSITIONS[i], 484, HEADERS[i], WIDTHS[i]);
        content.moveTo(32, 477);
        content.lineTo(807, 477);
        content.stroke();
        return content;
    }

    private void write(PDPageContentStream content, PDType1Font font, int size,
                       float x, float y, String text, float width) throws IOException {
        var sanitized = new StringBuilder();
        if (text != null) {
            for (int codePoint : text.replace('\n', ' ').replace('\r', ' ').codePoints().toArray()) {
                var value = new String(Character.toChars(codePoint));
                try {
                    font.getStringWidth(value);
                    sanitized.append(value);
                } catch (IllegalArgumentException error) {
                    sanitized.append('?');
                }
            }
        }
        var safe = sanitized.toString();
        while (!safe.isEmpty() && font.getStringWidth(safe) * size / 1000f > width)
            safe = safe.substring(0, safe.offsetByCodePoints(safe.length(), -1));
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(safe);
        content.endText();
    }
}
