package com.example.broilerfarm;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.logging.Logger;

@SpringBootApplication
public class BroilerFarmApplication {
    private static final Logger logger = Logger.getLogger(BroilerFarmApplication.class.getName());

    public static void main(String[] args) {
        logger.info("The broiler farms was started 🐔 🐔 🐔");


        try {
            Dotenv dotenv = Dotenv.configure()
                    .directory("./")
                    .ignoreIfMissing()
                    .load();

            // Setează variabilele în System properties
            dotenv.entries().forEach(entry -> {
                System.setProperty(entry.getKey(), entry.getValue());
                logger.info("Loaded env variable: " + entry.getKey() + " = " +
                        (entry.getKey().contains("PASSWORD") ? "***" : entry.getValue()));
            });

            logger.info("✅ .env file loaded successfully!");
        } catch (Exception e) {
            logger.warning("⚠️ Failed to load .env file: " + e.getMessage());
            logger.warning("Will try to use system environment variables or application.properties");
        }

        SpringApplication.run(BroilerFarmApplication.class, args);
    }
}