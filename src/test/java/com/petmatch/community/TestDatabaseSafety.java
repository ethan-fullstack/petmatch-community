package com.petmatch.community;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/** Fails before any beans or Hibernate initialization can touch the database. */
public class TestDatabaseSafety implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext context) {
        String url = context.getEnvironment().getRequiredProperty("spring.datasource.url");
        if (!url.matches("jdbc:mysql://[^/]+/[A-Za-z0-9_]+_test([?].*)?")) {
            throw new IllegalStateException("TEST_DB_URL must identify a dedicated MySQL schema ending in _test");
        }
    }
}
