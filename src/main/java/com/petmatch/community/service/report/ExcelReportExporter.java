package com.petmatch.community.service.report;

import com.petmatch.community.dto.report.*;
import java.io.*;
import java.util.List;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class ExcelReportExporter implements ReportExporter {
    @Override public ReportExportFormat format() { return ReportExportFormat.XLSX; }
    @Override public MediaType mediaType() {
        return MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @Override
    public byte[] export(SupportRequestReportFilter filter, List<ReportRow> rows, List<ReportSummary> summary) throws IOException {
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            var font = workbook.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(font);
            var dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("yyyy-mm-dd hh:mm"));

            var sheet = workbook.createSheet("Requests");
            var labels = new String[]{"ID", "Title", "Type", "Status", "Pet", "Service date"};
            var heading = sheet.createRow(0);
            for (int i = 0; i < labels.length; i++) {
                var cell = heading.createCell(i);
                cell.setCellValue(labels[i]);
                cell.setCellStyle(headerStyle);
            }
            int rowNumber = 1;
            for (var record : rows) {
                var row = sheet.createRow(rowNumber++);
                row.createCell(0).setCellValue(record.id());
                row.createCell(1).setCellValue(safeCellText(record.title()));
                row.createCell(2).setCellValue(record.supportType().name());
                row.createCell(3).setCellValue(record.status().name());
                row.createCell(4).setCellValue(safeCellText(record.petName()));
                var dateCell = row.createCell(5);
                dateCell.setCellValue(record.serviceDate());
                dateCell.setCellStyle(dateStyle);
            }
            int[] widths = {12, 54, 22, 23, 32, 24};
            for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i] * 256);
            sheet.createFreezePane(0, 1);
            if (!rows.isEmpty()) sheet.setAutoFilter(new CellRangeAddress(0, rows.size(), 0, 5));

            var totals = workbook.createSheet("Summary");
            var first = totals.createRow(0);
            first.createCell(0).setCellValue("Grouping");
            first.createCell(1).setCellValue(filter.getGrouping().name());
            first.createCell(2).setCellValue("Filtered total");
            first.createCell(3).setCellValue(rows.size());
            var names = totals.createRow(2);
            names.createCell(0).setCellValue("Group");
            names.createCell(1).setCellValue("Count");
            names.getCell(0).setCellStyle(headerStyle);
            names.getCell(1).setCellStyle(headerStyle);
            int index = 3;
            for (var entry : summary) {
                var row = totals.createRow(index++);
                row.createCell(0).setCellValue(entry.label());
                row.createCell(1).setCellValue(entry.total());
            }
            for (int i = 0; i < 4; i++) totals.setColumnWidth(i, 24 * 256);
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private String safeCellText(String text) {
        if (text == null) return "";
        var normalized = text.replace('\n', ' ').replace('\r', ' ');
        return !normalized.isEmpty() && "=+-@".indexOf(normalized.charAt(0)) >= 0 ? "'" + normalized : normalized;
    }
}
