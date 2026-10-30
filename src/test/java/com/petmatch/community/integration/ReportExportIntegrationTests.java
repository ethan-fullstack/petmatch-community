package com.petmatch.community.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.petmatch.community.model.*;
import com.petmatch.community.model.enums.*;
import com.petmatch.community.repository.*;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@org.springframework.test.context.ActiveProfiles("test")
@org.springframework.test.context.ContextConfiguration(initializers=com.petmatch.community.TestDatabaseSafety.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReportExportIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PetRepository pets;
    @Autowired SupportRequestRepository requests;

    @BeforeEach
    void prepare() {
        var admin = users.saveAndFlush(new User("Export admin", "export-admin@example.test", "{noop}unused"));
        admin.setRole(Role.ADMIN);
        var owner = users.saveAndFlush(new User("Export owner", "export-owner@example.test", "{noop}unused"));
        users.flush();
        var pet = pets.saveAndFlush(new Pet("Luna", "Dog", 3, "", owner));
        for (int i = 0; i < 26; i++) {
            var request = new SupportRequest("Pet walk " + i, "Description", i == 25 ? SupportType.FEEDING : SupportType.WALK,
                LocalDateTime.of(2030, 1, 10, 10, 0).plusHours(i), pet, owner);
            requests.save(request);
        }
        requests.flush();
    }

    @Test
    void pdfExportsFilteredRowsBeyondFirstPage() throws Exception {
        var result = mvc.perform(get("/admin/reports/export/pdf")
            .with(user("export-admin@example.test").roles("ADMIN"))
            .param("supportType", "WALK")
            .param("grouping", "SUPPORT_TYPE"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/pdf"))
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"petmatch-report.pdf\""))
            .andReturn();
        var bytes = result.getResponse().getContentAsByteArray();
        assertEquals("%PDF", new String(bytes, 0, 4, StandardCharsets.US_ASCII));
        try (var document = Loader.loadPDF(bytes)) {
            var text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("Filtered total: 25"));
            assertTrue(text.contains("Pet walk 24"));
            assertFalse(text.contains("Pet walk 25"));
            assertTrue(text.contains("WALK: 25"));
        }
    }

    @Test
    void excelExportsAllRowsAndNativeDates() throws Exception {
        var result = mvc.perform(get("/admin/reports/export/xlsx")
            .with(user("export-admin@example.test").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .andReturn();
        try (var workbook = WorkbookFactory.create(
                new ByteArrayInputStream(result.getResponse().getContentAsByteArray()))) {
            var details = workbook.getSheet("Requests");
            assertEquals(27, details.getPhysicalNumberOfRows());
            assertEquals("Pet walk 25", details.getRow(26).getCell(1).getStringCellValue());
            assertTrue(org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(details.getRow(26).getCell(5)));
            assertEquals(26.0, workbook.getSheet("Summary").getRow(0).getCell(3).getNumericCellValue());
        }
    }

    @Test
    void exportRejectsNonAdminInvalidFiltersAndUnknownFormats() throws Exception {
        mvc.perform(get("/admin/reports/export/pdf").with(user("export-owner@example.test")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/admin/reports/export/xlsx")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/reports/export/pdf")
            .with(user("export-admin@example.test").roles("ADMIN"))
            .param("fromDate", "2030-02-01").param("toDate", "2030-01-01"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/admin/reports/export/html")
            .with(user("export-admin@example.test").roles("ADMIN")))
            .andExpect(status().isNotFound());
    }

    @Test
    void reportPageDisplaysBothExportLinks() throws Exception {
        mvc.perform(get("/admin/reports").with(user("export-admin@example.test").roles("ADMIN"))
            .param("supportType", "WALK"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Descargar PDF")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Descargar Excel")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("supportType=WALK")));
    }
}
