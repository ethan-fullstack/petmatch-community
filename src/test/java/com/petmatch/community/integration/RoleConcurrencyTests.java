package com.petmatch.community.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.petmatch.community.model.User;
import com.petmatch.community.model.enums.Role;
import com.petmatch.community.repository.*;
import com.petmatch.community.service.AdminUserService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.*;

@org.springframework.test.context.ActiveProfiles("test")
@org.springframework.test.context.ContextConfiguration(initializers=com.petmatch.community.TestDatabaseSafety.class)
@SpringBootTest
class RoleConcurrencyTests {
    @Autowired UserRepository users;
    @Autowired RoleChangeRepository changes;
    @Autowired AdminUserService service;
    @Autowired PlatformTransactionManager manager;

    @Test
    void simultaneousSelfDemotionsLeaveOneActiveAdmin() throws Exception {
        var tx = new TransactionTemplate(manager);
        List<User> admins =
                tx.execute(
                        status -> {
                            var a =
                                    users.saveAndFlush(
                                            new User(
                                                    "A",
                                                    "concurrent-a@example.test",
                                                    "{noop}unused"));
                            a.setRole(Role.ADMIN);
                            var b =
                                    users.saveAndFlush(
                                            new User(
                                                    "B",
                                                    "concurrent-b@example.test",
                                                    "{noop}unused"));
                            b.setRole(Role.ADMIN);
                            users.flush();
                            return List.of(a, b);
                        });
        var gate = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var jobs =
                    admins.stream()
                            .map(
                                    a ->
                                            pool.submit(
                                                    () -> {
                                                        gate.await();
                                                        try {
                                                            service.changeRole(
                                                                    a.getEmail(),
                                                                    a.getId(),
                                                                    Role.USER);
                                                            return true;
                                                        } catch (IllegalArgumentException e) {
                                                            return false;
                                                        }
                                                    }))
                            .toList();
            gate.countDown();
            int success = 0;
            for (var result : jobs) if (result.get(20, TimeUnit.SECONDS)) success++;
            assertEquals(1, success);
            assertEquals(
                    1,
                    users.findAllById(admins.stream().map(User::getId).toList()).stream()
                            .filter(a -> a.getRole() == Role.ADMIN)
                            .count());
        } finally {
            tx.executeWithoutResult(
                    status -> {
                        changes.deleteAll(
                                changes.findAll().stream()
                                        .filter(
                                                c ->
                                                        admins.stream()
                                                                .anyMatch(
                                                                        a ->
                                                                                a.getId()
                                                                                        .equals(
                                                                                                c
                                                                                                        .getTargetId())))
                                        .toList());
                        users.deleteAllById(admins.stream().map(User::getId).toList());
                    });
        }
    }
}
