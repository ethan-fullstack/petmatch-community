package com.petmatch.community.service.report;

public class ReportExportLimitExceededException extends RuntimeException {
    public ReportExportLimitExceededException(int maximumRows) {
        super("Too many matching rows. Maximum: " + maximumRows);
    }
}
