package com.ppms.hatchery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"com.ppms.hatchery", "com.ppms.shared"})
@EnableJpaRepositories(basePackages = {"com.ppms.hatchery", "com.ppms.shared"})
@EntityScan(basePackages = {"com.ppms.hatchery", "com.ppms.shared"})
public class HatcheryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(HatcheryServiceApplication.class, args);
    }

}
