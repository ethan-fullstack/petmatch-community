package com.petmatch.community.controller;

import com.petmatch.community.dto.report.*;
import com.petmatch.community.service.report.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/admin/reports/export")
public class AdminReportExportController {
    private final ReportExportService service;

    public AdminReportExportController(ReportExportService service) {
        this.service = service;
    }

    @GetMapping("/{format}")
    public ResponseEntity<byte[]> export(
            @PathVariable String format,
            @Valid @ModelAttribute("filter") SupportRequestReportFilter filter,
            BindingResult errors) {
        if (errors.hasErrors()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filters");
        ReportExportFormat parsed;
        try {
            parsed = ReportExportFormat.parse(format);
        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unsupported format", error);
        }
        try {
            var file = service.export(filter, parsed);
            return ResponseEntity.ok()
                .contentType(file.mediaType())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.filename() + "\"")
                .cacheControl(CacheControl.noStore())
                .body(file.content());
        } catch (ReportExportLimitExceededException error) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Apply more restrictive filters", error);
        }
    }
}
