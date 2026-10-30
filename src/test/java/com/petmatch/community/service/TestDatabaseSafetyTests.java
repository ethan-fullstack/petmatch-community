package com.petmatch.community.service;

import static org.junit.jupiter.api.Assertions.*;
import com.petmatch.community.TestDatabaseSafety;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.env.MockEnvironment;

class TestDatabaseSafetyTests {
    @Test void rejectsDevelopmentSchemaBeforeBeanInitialization() {
        try (var context = new GenericApplicationContext()) {
            context.setEnvironment(new MockEnvironment().withProperty("spring.datasource.url", "jdbc:mysql://localhost:3306/petmatch_community"));
            assertThrows(IllegalStateException.class, () -> new TestDatabaseSafety().initialize(context));
        }
    }
    @Test void acceptsOnlyDedicatedTestSchemaAndOptionalQuery() {
        try (var context = new GenericApplicationContext()) {
            context.setEnvironment(new MockEnvironment().withProperty("spring.datasource.url", "jdbc:mysql://localhost:3306/petmatch_test?serverTimezone=UTC"));
            assertDoesNotThrow(() -> new TestDatabaseSafety().initialize(context));
        }
    }
}
