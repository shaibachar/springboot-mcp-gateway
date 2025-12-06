package com.shaibachar.springbootmcplib.integration;

import com.shaibachar.springbootmcplib.config.McpAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

/**
 * Test Spring Boot application for integration testing.
 */
@SpringBootApplication
@Import(McpAutoConfiguration.class)
@ComponentScan(basePackages = {
    "com.shaibachar.springbootmcplib.integration",
    "com.shaibachar.springbootmcplib"
})
public class TestApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
