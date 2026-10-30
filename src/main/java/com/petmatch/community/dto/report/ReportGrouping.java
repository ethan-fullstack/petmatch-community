package com.petmatch.community.dto.report;

public enum ReportGrouping {
    STATUS("status"),
    SUPPORT_TYPE("supportType");

    private final String entityAttribute;

    ReportGrouping(String entityAttribute) {
        this.entityAttribute = entityAttribute;
    }

    public String entityAttribute() {
        return entityAttribute;
    }
}
