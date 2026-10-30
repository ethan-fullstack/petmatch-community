package com.petmatch.community.service;

import static org.junit.jupiter.api.Assertions.*;

import com.petmatch.community.dto.report.*;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

class ReportGroupingTests {
    @Test
    void groupingUsesOnlyAllowedEntityAttributes() {
        assertEquals("status", ReportGrouping.STATUS.entityAttribute());
        assertEquals("supportType", ReportGrouping.SUPPORT_TYPE.entityAttribute());
    }

    @Test
    void dateNormalizationHandlesLeapDayAndRejectsOverflow() {
        var filter = new SupportRequestReportFilter();
        filter.setToDate(LocalDate.of(2032, 2, 29));
        assertEquals(LocalDate.of(2032, 3, 1).atStartOfDay(), filter.endExclusive());
        filter.setToDate(LocalDate.MAX);
        assertFalse(filter.isValidRange());
        assertThrows(IllegalArgumentException.class, filter::validate);
    }
}
