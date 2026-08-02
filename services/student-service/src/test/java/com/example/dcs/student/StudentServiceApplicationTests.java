package com.example.dcs.student;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Basic application context test for Student Service.
 * Ensures the Spring application context loads successfully.
 */
@SpringBootTest
@ActiveProfiles("test")
class StudentServiceApplicationTests {

    @Test
    void contextLoads() {
        // This test ensures that the Spring application context loads successfully
        // If the context fails to load, this test will fail
    }
}

