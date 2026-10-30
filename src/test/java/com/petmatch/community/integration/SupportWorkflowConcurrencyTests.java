package com.petmatch.community.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.petmatch.community.exception.SupportApplicationStateException;
import com.petmatch.community.exception.SupportRequestStateException;
import com.petmatch.community.model.*;
import com.petmatch.community.model.enums.*;
import com.petmatch.community.repository.*;
import com.petmatch.community.service.SupportApplicationService;
import com.petmatch.community.service.SupportRequestService;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@org.springframework.test.context.ActiveProfiles("test")
@org.springframework.test.context.ContextConfiguration(initializers=com.petmatch.community.TestDatabaseSafety.class)
@SpringBootTest
class SupportWorkflowConcurrencyTests {
    @Autowired UserRepository users;
    @Autowired PetRepository pets;
    @Autowired SupportRequestRepository requests;
    @Autowired SupportApplicationRepository applications;
    @Autowired SupportRequestService requestService;
    @Autowired SupportApplicationService applicationService;
    @Autowired PlatformTransactionManager transactionManager;

    private record Fixture(Long ownerId, Long applicantId, Long petId, Long requestId,
                           Long applicationId, String ownerEmail) {}

    @Test
    @Timeout(45)
    void simultaneousAcceptanceAndCancellationLeaveConsistentRequestAndApplicationStates() throws Exception {
        var transactions = new TransactionTemplate(transactionManager);
        Fixture fixture = createFixture(transactions);
        try {
            Authentication owner = new UsernamePasswordAuthenticationToken(
                    fixture.ownerEmail(), "unused", AuthorityUtils.createAuthorityList("ROLE_USER"));
            var gate = new CountDownLatch(1);
            try (var executor = Executors.newFixedThreadPool(2)) {
                Future<Boolean> accepted = executor.submit(() -> {
                    gate.await();
                    try {
                        applicationService.accept(fixture.applicationId(), owner);
                        return true;
                    } catch (SupportApplicationStateException exception) {
                        return false;
                    }
                });
                Future<Boolean> cancelled = executor.submit(() -> {
                    gate.await();
                    try {
                        requestService.cancel(fixture.requestId(), owner);
                        return true;
                    } catch (SupportRequestStateException exception) {
                        return false;
                    }
                });
                gate.countDown();
                boolean acceptedSuccessfully = accepted.get(25, TimeUnit.SECONDS);
                boolean cancelledSuccessfully = cancelled.get(25, TimeUnit.SECONDS);
                assertNotEquals(acceptedSuccessfully, cancelledSuccessfully);

                transactions.executeWithoutResult(status -> {
                    var request = requests.findById(fixture.requestId()).orElseThrow();
                    var application = applications.findById(fixture.applicationId()).orElseThrow();
                    if (acceptedSuccessfully) {
                        assertEquals(SupportRequestStatus.IN_PROGRESS, request.getStatus());
                        assertEquals(SupportApplicationStatus.ACCEPTED, application.getStatus());
                    } else {
                        assertEquals(SupportRequestStatus.CANCELLED, request.getStatus());
                        assertEquals(SupportApplicationStatus.REJECTED, application.getStatus());
                    }
                });
            }
        } finally {
            transactions.executeWithoutResult(status -> {
                applications.deleteById(fixture.applicationId());
                requests.deleteById(fixture.requestId());
                pets.deleteById(fixture.petId());
                users.deleteById(fixture.applicantId());
                users.deleteById(fixture.ownerId());
            });
        }
    }

    private Fixture createFixture(TransactionTemplate transactions) {
        return transactions.execute(status -> {
            String suffix = UUID.randomUUID().toString();
            String ownerEmail = "workflow-owner-" + suffix + "@example.test";
            var owner = users.saveAndFlush(new User("Workflow owner", ownerEmail, "{noop}unused"));
            var applicant = users.saveAndFlush(new User(
                    "Workflow applicant", "workflow-applicant-" + suffix + "@example.test", "{noop}unused"));
            var pet = pets.saveAndFlush(new Pet("Luna", "Dog", 3, "", owner));
            var request = requests.saveAndFlush(new SupportRequest(
                    "Help with Luna", "Walk", SupportType.WALK,
                    LocalDateTime.now().plusDays(10), pet, owner));
            var application = applications.saveAndFlush(new SupportApplication(null, applicant, request));
            return new Fixture(owner.getId(), applicant.getId(), pet.getId(),
                    request.getId(), application.getId(), ownerEmail);
        });
    }
}
