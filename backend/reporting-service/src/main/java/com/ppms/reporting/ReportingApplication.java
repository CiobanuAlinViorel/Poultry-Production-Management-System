package com.ppms.reporting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"com.ppms.reporting", "com.ppms.shared"})
@EnableJpaRepositories(basePackages = {"com.ppms.reporting", "com.ppms.shared"})
@EntityScan(basePackages = {"com.ppms.reporting", "com.ppms.shared"})
public class ReportingApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReportingApplication.class, args);
    }
}
