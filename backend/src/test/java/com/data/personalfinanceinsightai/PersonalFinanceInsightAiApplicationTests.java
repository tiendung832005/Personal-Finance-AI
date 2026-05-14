package com.data.personalfinanceinsightai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Skips Flyway checksum validation so a shared local MySQL (same URL as dev) still loads the context
 * after a versioned migration file is edited post-apply. For normal app runs, prefer {@code flyway repair}
 * (or update {@code flyway_schema_history.checksum}) so validation stays enabled.
 */
@SpringBootTest
@TestPropertySource(properties = "spring.flyway.validate-on-migrate=false")
class PersonalFinanceInsightAiApplicationTests {

    @Test
    void contextLoads() {
    }

}
