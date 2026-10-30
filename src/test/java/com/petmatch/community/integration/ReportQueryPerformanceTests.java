package com.petmatch.community.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.petmatch.community.dto.report.SupportRequestReportFilter;
import com.petmatch.community.model.*;
import com.petmatch.community.model.enums.*;
import com.petmatch.community.repository.*;
import com.petmatch.community.service.report.SupportRequestReportService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@org.springframework.test.context.ActiveProfiles("test")
@org.springframework.test.context.ContextConfiguration(initializers=com.petmatch.community.TestDatabaseSafety.class)
@SpringBootTest
@Transactional
class ReportQueryPerformanceTests {
    private static final int ROWS = 120;
    private static final int SAMPLES = 25;

    @Autowired SupportRequestReportService service;
    @Autowired UserRepository users;
    @Autowired PetRepository pets;
    @Autowired SupportRequestRepository requests;

    @Test
    void recordObservedReportQueryLatencyWithoutAssumingAProductionThreshold() throws Exception {
        var owner = users.saveAndFlush(new User(
                "Performance owner", "performance-owner@example.test", "{noop}unused"));
        var pet = pets.saveAndFlush(new Pet("Luna", "Dog", 3, "", owner));
        for (int index = 0; index < ROWS; index++) {
            requests.save(new SupportRequest(
                    "Performance request " + index, "Synthetic data", SupportType.WALK,
                    LocalDateTime.of(2030, 1, 10, 10, 0).plusMinutes(index), pet, owner));
        }
        requests.flush();

        var filter = new SupportRequestReportFilter();
        filter.setStatus(SupportRequestStatus.OPEN);
        filter.setSupportType(SupportType.WALK);
        for (int index = 0; index < 5; index++) {
            assertEquals(ROWS, service.search(filter, 0).rows().getTotalElements());
        }

        long[] durations = new long[SAMPLES];
        for (int index = 0; index < SAMPLES; index++) {
            long started = System.nanoTime();
            var report = service.search(filter, 0);
            durations[index] = System.nanoTime() - started;
            assertEquals(ROWS, report.rows().getTotalElements());
            assertEquals(20, report.rows().getNumberOfElements());
        }
        Arrays.sort(durations);
        double medianMs = durations[SAMPLES / 2] / 1_000_000.0;
        double p95Ms = durations[(int) Math.ceil(SAMPLES * .95) - 1] / 1_000_000.0;
        String sha = System.getenv("GITHUB_SHA");
        if (sha == null) sha = "local";
        String json = String.format(Locale.ROOT,
                "{\"scope\":\"service-level-report-query\",\"fixture_rows\":%d,"
                        + "\"warmups\":5,\"samples\":%d,\"median_ms\":%.3f,"
                        + "\"p95_ms\":%.3f,\"commit\":\"%s\"}%n",
                ROWS, SAMPLES, medianMs, p95Ms, sha);
        Path destination = Path.of("target", "report-query-performance.json");
        Files.createDirectories(destination.getParent());
        Files.writeString(destination, json);
        System.out.println("REPORT_QUERY_BASELINE " + json.trim());
    }
}
