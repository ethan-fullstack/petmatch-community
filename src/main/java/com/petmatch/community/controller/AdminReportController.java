package com.petmatch.community.controller;

import com.petmatch.community.dto.report.*;
import com.petmatch.community.model.enums.*;
import com.petmatch.community.service.report.SupportRequestReportService;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
public class AdminReportController {
    private final SupportRequestReportService service;

    public AdminReportController(SupportRequestReportService service) {
        this.service = service;
    }

    @GetMapping("/admin/reports")
    public String report(
            @Valid @ModelAttribute("filter") SupportRequestReportFilter filter,
            BindingResult errors,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        model.addAttribute("statuses", SupportRequestStatus.values());
        model.addAttribute("types", SupportType.values());
        model.addAttribute("groupings", ReportGrouping.values());
        if (!errors.hasErrors()) model.addAttribute("report", service.search(filter, page));
        return "admin/reports";
    }
}
