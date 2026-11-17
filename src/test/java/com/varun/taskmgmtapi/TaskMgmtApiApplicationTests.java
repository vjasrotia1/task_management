package com.varun.taskmgmtapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
//What does @ActiveProfiles("test") do?
//It tells Spring Boot to load application-test.properties when running this test class. Your normal application will continue using the MySQL configuration.
class TaskMgmtApiApplicationTests {

    @Test
    void contextLoads() {
    }

}
