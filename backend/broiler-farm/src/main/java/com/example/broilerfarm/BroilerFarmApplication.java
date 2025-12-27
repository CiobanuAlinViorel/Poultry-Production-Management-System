package com.example.broilerfarm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.logging.Logger;

@SpringBootApplication
public class BroilerFarmApplication {
    private static final Logger logger = Logger.getLogger(BroilerFarmApplication.class.getName());

    public static void main(String[] args) {
        logger.info("The broiler farms was started 🐔 🐔 🐔");

        SpringApplication.run(BroilerFarmApplication.class, args);
    }
}