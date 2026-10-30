package com.petmatch.community.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.petmatch.community.dto.report.*;
import com.petmatch.community.model.*;
import com.petmatch.community.model.enums.*;
import com.petmatch.community.repository.*;
import com.petmatch.community.service.report.*;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;

@org.springframework.test.context.ActiveProfiles("test")
@org.springframework.test.context.ContextConfiguration(initializers=com.petmatch.community.TestDatabaseSafety.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReportIntegrationTests {
    @Autowired SupportRequestReportService service;
    @Autowired UserRepository users;
    @Autowired PetRepository pets;
    @Autowired SupportRequestRepository requests;
    @Autowired MockMvc mvc;
    User owner, other;
    Pet pet, otherPet;

    @BeforeEach
    void setup() {
        owner = users.saveAndFlush(new User("Owner", "report-owner@example.test", "{noop}unused"));
        other = users.saveAndFlush(new User("Other", "report-other@example.test", "{noop}unused"));
        var admin = users.saveAndFlush(new User("Report Admin", "report-admin@example.test", "{noop}unused"));
        admin.setRole(Role.ADMIN);
        users.flush();
        pet = pets.saveAndFlush(new Pet("Luna", "Perro", 2, "", owner));
        otherPet = pets.saveAndFlush(new Pet("Sol", "Gato", 3, "", other));
        add(SupportType.WALK, SupportRequestStatus.OPEN, "2030-01-10T00:00:00", pet, owner);
        add(
                SupportType.WALK,
                SupportRequestStatus.COMPLETED,
                "2030-01-10T23:59:59",
                otherPet,
                other);
        add(SupportType.FEEDING, SupportRequestStatus.OPEN, "2030-01-11T00:00:00", pet, owner);
        add(SupportType.WALK, SupportRequestStatus.CANCELLED, "2030-01-09T23:59:59", pet, owner);
    }

    void add(SupportType type, SupportRequestStatus status, String date, Pet p, User u) {
        var r =
                new SupportRequest(
                        "Solicitud", "Descripción", type, LocalDateTime.parse(date), p, u);
        r.setStatus(status);
        requests.saveAndFlush(r);
    }

    @Test
    void reportsRenderResponsiveFiltersAndLocalStyles() throws Exception {
        var admin = users.findByEmailIgnoreCase("report-admin@example.test").orElseThrow();
        mvc.perform(get("/admin/reports").with(user(admin.getEmail()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/css/application.css")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("pm-filter-grid")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("pm-panel")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Todos los estados")));
    }

    @Test
    void unfilteredIncludesAllOwnersAndStates() {
        var r = service.search(new SupportRequestReportFilter(), 0);
        assertEquals(4, r.rows().getTotalElements());
        assertEquals(4, r.summary().stream().mapToLong(ReportSummary::total).sum());
        assertTrue(r.rows().stream().anyMatch(x -> x.petName().equals("Sol")));
    }

    @Test
    void filtersIndividuallyAndTogether() {
        var f = new SupportRequestReportFilter();
        f.setStatus(SupportRequestStatus.OPEN);
        assertEquals(2, service.search(f, 0).rows().getTotalElements());
        f.setSupportType(SupportType.WALK);
        assertEquals(1, service.search(f, 0).rows().getTotalElements());
        f.setFromDate(LocalDate.of(2030, 1, 10));
        f.setToDate(LocalDate.of(2030, 1, 10));
        assertEquals(1, service.search(f, 0).rows().getTotalElements());
    }

    @Test
    void dateBoundsIncludeWholeLastDayAndExcludeNextMidnight() {
        var f = new SupportRequestReportFilter();
        f.setFromDate(LocalDate.of(2030, 1, 10));
        f.setToDate(LocalDate.of(2030, 1, 10));
        var r = service.search(f, 0);
        assertEquals(2, r.rows().getTotalElements());
        assertEquals("Sol", r.rows().getContent().get(1).petName());
    }

    @Test
    void bothStrategiesAggregateSameFilteredPopulation() {
        var f = new SupportRequestReportFilter();
        f.setSupportType(SupportType.WALK);
        var status = service.search(f, 0);
        assertEquals(3, status.summary().size());
        f.setGrouping(ReportGrouping.SUPPORT_TYPE);
        var type = service.search(f, 0);
        assertEquals(java.util.List.of(new ReportSummary("WALK", 3L)), type.summary());
        assertEquals(status.rows().getTotalElements(), type.rows().getTotalElements());
    }

    @Test
    void totalsAreNotLimitedToPage() {
        for (int i = 0; i < 22; i++)
            add(SupportType.WALK, SupportRequestStatus.OPEN, "2030-01-12T12:00:00", pet, owner);
        var f = new SupportRequestReportFilter();
        var first = service.search(f, 0);
        var second = service.search(f, 1);
        assertEquals(20, first.rows().getNumberOfElements());
        assertEquals(6, second.rows().getNumberOfElements());
        assertEquals(26, second.rows().getTotalElements());
        assertEquals(26, second.summary().stream().mapToLong(ReportSummary::total).sum());
    }

    @Test
    void emptyResultAndInvalidRange() {
        var f = new SupportRequestReportFilter();
        f.setSupportType(SupportType.OTHER);
        assertTrue(service.search(f, 0).rows().isEmpty());
        assertTrue(service.search(f, 0).summary().isEmpty());
        f.setFromDate(LocalDate.of(2030, 2, 1));
        f.setToDate(LocalDate.of(2030, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> service.search(f, 0));
    }

    @Test
    void adminViewAndValidationErrorsRender() throws Exception {
        mvc.perform(
                        get("/admin/reports")
                                .with(user("report-admin@example.test").roles("ADMIN"))
                                .param("supportType", "WALK")
                                .param("fromDate", "2030-01-10")
                                .param("toDate", "2030-01-10"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sol")));
        mvc.perform(
                        get("/admin/reports")
                                .with(user("report-admin@example.test").roles("ADMIN"))
                                .param("fromDate", "2030-02-01")
                                .param("toDate", "2030-01-01"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasErrors("filter"));
        mvc.perform(get("/admin/reports").with(user("report-admin@example.test").roles("ADMIN")).param("grouping", ""))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasErrors("filter"));
        mvc.perform(
                        get("/admin/reports")
                                .with(user("report-admin@example.test").roles("ADMIN"))
                                .param("status", "INVALID"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasErrors("filter")).andExpect(content().string(org.hamcrest.Matchers.containsString("Selecciona un estado válido.")));
    }
}
