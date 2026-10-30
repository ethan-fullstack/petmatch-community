package com.petmatch.community.dto.report;

public enum ReportExportFormat {
    PDF("pdf"), XLSX("xlsx");
    private final String extension;
    ReportExportFormat(String extension) { this.extension = extension; }
    public String extension() { return extension; }
    public static ReportExportFormat parse(String value) {
        for (var format : values()) if (format.extension.equalsIgnoreCase(value)) return format;
        throw new IllegalArgumentException("Unsupported export format");
    }
}
