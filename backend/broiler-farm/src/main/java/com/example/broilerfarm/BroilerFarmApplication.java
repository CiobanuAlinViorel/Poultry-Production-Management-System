package com.example.broilerfarm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.logging.Logger;

@SpringBootApplication
@EnableScheduling
public class BroilerFarmApplication {
    private static final Logger logger = Logger.getLogger(BroilerFarmApplication.class.getName());

    public static void main(String[] args) {
        logger.info("The broiler farms was started 🐔 🐔 🐔");

        SpringApplication.run(BroilerFarmApplication.class, args);
    }
}