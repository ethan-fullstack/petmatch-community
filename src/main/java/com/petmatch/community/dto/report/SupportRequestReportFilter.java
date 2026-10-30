package com.petmatch.community.dto.report;

import com.petmatch.community.model.enums.*;

import jakarta.validation.constraints.AssertTrue;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class SupportRequestReportFilter {
    private SupportRequestStatus status;
    private SupportType supportType;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;

    @jakarta.validation.constraints.NotNull(message = "Selecciona una agrupación válida")
    private ReportGrouping grouping = ReportGrouping.STATUS;

    @AssertTrue(
            message =
                    "El rango debe ser válido: fecha inicial ≤ final y fecha final anterior a"
                        + " 9999-12-31")
    public boolean isValidRange() {
        return (fromDate == null || toDate == null || !fromDate.isAfter(toDate))
                && validDate(fromDate)
                && validDate(toDate);
    }

    private boolean validDate(LocalDate date) {
        return date == null
                || (date.getYear() >= 1000 && date.isBefore(LocalDate.of(9999, 12, 31)));
    }

    public void validate() {
        if (!isValidRange() || grouping == null)
            throw new IllegalArgumentException("Filtros inválidos");
    }

    public LocalDateTime start() {
        return fromDate == null ? null : fromDate.atStartOfDay();
    }

    public LocalDateTime endExclusive() {
        return toDate == null ? null : toDate.plusDays(1).atStartOfDay();
    }

    public SupportRequestStatus getStatus() {
        return status;
    }

    public void setStatus(SupportRequestStatus v) {
        status = v;
    }

    public SupportType getSupportType() {
        return supportType;
    }

    public void setSupportType(SupportType v) {
        supportType = v;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDate v) {
        fromDate = v;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public void setToDate(LocalDate v) {
        toDate = v;
    }

    public ReportGrouping getGrouping() {
        return grouping;
    }

    public void setGrouping(ReportGrouping v) {
        grouping = v;
    }
}
