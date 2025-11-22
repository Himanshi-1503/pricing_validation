package com.cme.pricing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for Pricing Data Validation & Reporting Utility
 */
@SpringBootApplication
public class PricingValidationApplication {

    public static void main(String[] args) {
        // Check if CLI mode is requested
        if (args.length > 0 && args[0].equals("--cli")) {
            System.setProperty("spring.main.web-application-type", "none");
            SpringApplication app = new SpringApplication(PricingValidationApplication.class);
            app.run(args);
        } else {
            SpringApplication.run(PricingValidationApplication.class, args);
            System.out.println("Pricing Data Validation & Reporting Utility is running...");
            System.out.println("API endpoints available at: http://localhost:8080");
            System.out.println("\nTo use CLI mode, run with: java -jar target/pricing-validation-1.0.0.jar --cli");
        }
    }
}

