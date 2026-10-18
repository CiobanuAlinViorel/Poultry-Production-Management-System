package com.ppms.slaughterhouse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"com.ppms.slaughterhouse", "com.ppms.shared"})
@EnableJpaRepositories(basePackages = {"com.ppms.slaughterhouse", "com.ppms.shared"})
@EntityScan(basePackages = {"com.ppms.slaughterhouse", "com.ppms.shared"})
public class SlaughterhouseServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SlaughterhouseServiceApplication.class, args);
    }

}
