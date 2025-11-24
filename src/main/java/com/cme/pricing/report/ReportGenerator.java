package com.cme.pricing.report;

import com.cme.pricing.model.PricingRecord;
import com.cme.pricing.model.ValidationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Generator for validation reports
 */
@Component
public class ReportGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(ReportGenerator.class);

    /**
     * Generates a text report and saves it to a file
     */
    public String generateTextReport(ValidationReport report, String outputPath) throws IOException {
        logger.info("Generating text report to: {}", outputPath);
        
        StringBuilder reportContent = new StringBuilder();
        
        // Header
        reportContent.append("=".repeat(43)).append("\n");
        reportContent.append("\n");
        reportContent.append("Pricing Data Validation Report\n");
        reportContent.append("\n");
        reportContent.append("=".repeat(43)).append("\n");
        reportContent.append("\n");
        
        // Summary
        reportContent.append("Total Records: ").append(report.getTotalRecords()).append("\n");
        reportContent.append("\n");
        reportContent.append("Valid Records: ").append(report.getValidRecords()).append("\n");
        reportContent.append("\n");
        reportContent.append("Invalid Records: ").append(report.getInvalidRecords()).append("\n");
        reportContent.append("\n");
        reportContent.append("Duplicate Records: ").append(report.getDuplicateRecords()).append("\n");
        reportContent.append("\n");
        
        // Calculate total missing values
        int totalMissing = report.getMissingPriceRecords() + 
                          report.getMissingInstrumentGuidRecords() + 
                          report.getMissingTradeDateRecords() + 
                          report.getMissingExchangeRecords() + 
                          report.getMissingProductTypeRecords();
        reportContent.append("Missing Values: ").append(totalMissing).append("\n");
        reportContent.append("\n");
        
        // Error Breakdown
        reportContent.append("Error Breakdown:\n");
        reportContent.append("\n");
        reportContent.append("-".repeat(27)).append("\n");
        reportContent.append("\n");
        
        if (report.getMissingPriceRecords() > 0) {
            reportContent.append("Missing Price: ").append(report.getMissingPriceRecords()).append("\n");
        }
        if (report.getInvalidPriceFormatRecords() > 0) {
            reportContent.append("Invalid Price Format: ").append(report.getInvalidPriceFormatRecords()).append("\n");
        }
        if (report.getMissingInstrumentGuidRecords() > 0) {
            reportContent.append("Missing instrument_guid: ").append(report.getMissingInstrumentGuidRecords()).append("\n");
        }
        if (report.getMissingTradeDateRecords() > 0) {
            reportContent.append("Missing trade_date: ").append(report.getMissingTradeDateRecords()).append("\n");
        }
        if (report.getMissingExchangeRecords() > 0) {
            reportContent.append("Missing exchange: ").append(report.getMissingExchangeRecords()).append("\n");
        }
        if (report.getMissingProductTypeRecords() > 0) {
            reportContent.append("Missing product_type: ").append(report.getMissingProductTypeRecords()).append("\n");
        }
        if (report.getInvalidExchangeRecords() > 0) {
            reportContent.append("Invalid exchange: ").append(report.getInvalidExchangeRecords()).append("\n");
        }
        if (report.getInvalidProductTypeRecords() > 0) {
            reportContent.append("Invalid product_type: ").append(report.getInvalidProductTypeRecords()).append("\n");
        }
        if (report.getDuplicateRecords() > 0) {
            reportContent.append("Duplicate Records: ").append(report.getDuplicateRecords()).append("\n");
        }
        
        reportContent.append("\n");
        reportContent.append("-".repeat(27)).append("\n");
        
        // Invalid Records Details
        if (!report.getInvalidRecordsList().isEmpty()) {
            reportContent.append("INVALID RECORDS DETAILS\n");
            reportContent.append("-".repeat(80)).append("\n");
            for (PricingRecord record : report.getInvalidRecordsList()) {
                String guid = record.getInstrumentGuid() != null && !record.getInstrumentGuid().trim().isEmpty()
                    ? record.getInstrumentGuid() : "";
                String date = record.getTradeDate() != null ? record.getTradeDate().toString() : "";
                String price = record.getPrice() != null ? String.valueOf(record.getPrice()) : "";
                String exchange = record.getExchange() != null && !record.getExchange().trim().isEmpty()
                    ? record.getExchange() : "";
                String productType = record.getProductType() != null && !record.getProductType().trim().isEmpty()
                    ? record.getProductType() : "";
                String error = record.getValidationError() != null ? record.getValidationError() : "";
                
                reportContent.append(String.format("Instrument GUID: %s\n", guid));
                reportContent.append(String.format("  Trade Date:    %s\n", date));
                reportContent.append(String.format("  Price:         %s\n", price));
                reportContent.append(String.format("  Exchange:      %s\n", exchange));
                reportContent.append(String.format("  Product Type:  %s\n", productType));
                reportContent.append(String.format("  Error:         %s\n", error));
                reportContent.append("\n");
            }
        }
        
        // Duplicate Records Details
        List<PricingRecord> duplicateRecords = new java.util.ArrayList<>();
        for (PricingRecord record : report.getInvalidRecordsList()) {
            if (record.getValidationError() != null && record.getValidationError().contains("Duplicate")) {
                duplicateRecords.add(record);
            }
        }
        
        if (!duplicateRecords.isEmpty()) {
            reportContent.append("DUPLICATE RECORDS DETAILS\n");
            reportContent.append("-".repeat(80)).append("\n");
            for (PricingRecord record : duplicateRecords) {
                reportContent.append(String.format("Instrument GUID: %s\n", 
                    record.getInstrumentGuid() != null && !record.getInstrumentGuid().trim().isEmpty() 
                        ? record.getInstrumentGuid() : ""));
                reportContent.append(String.format("  Trade Date:    %s\n", 
                    record.getTradeDate() != null ? record.getTradeDate().toString() : ""));
                // Format price
                String price;
                if (record.getPrice() != null) {
                    price = String.format("%.2f", record.getPrice());
                } else {
                    String error = record.getValidationError();
                    if (error != null && error.contains("Invalid price format: ")) {
                        int startIndex = error.indexOf("Invalid price format: ") + "Invalid price format: ".length();
                        int endIndex = error.indexOf(";", startIndex);
                        price = (endIndex == -1) ? error.substring(startIndex).trim() 
                                : error.substring(startIndex, endIndex).trim();
                    } else {
                        price = "";
                    }
                }
                reportContent.append(String.format("  Price:         %s\n", price));
                reportContent.append(String.format("  Exchange:      %s\n", 
                    record.getExchange() != null && !record.getExchange().trim().isEmpty() 
                        ? record.getExchange() : ""));
                reportContent.append(String.format("  Product Type:  %s\n", 
                    record.getProductType() != null && !record.getProductType().trim().isEmpty() 
                        ? record.getProductType() : ""));
                reportContent.append(String.format("  Error:         %s\n", 
                    record.getValidationError() != null ? record.getValidationError() : ""));
                reportContent.append("\n");
            }
        }
        
        // Missing Values Details
        if (totalMissing > 0) {
            reportContent.append("MISSING VALUES DETAILS\n");
            reportContent.append("-".repeat(80)).append("\n");
            
            // Missing Price Records
            if (report.getMissingPriceRecords() > 0) {
                reportContent.append("Missing Price Records:\n");
                for (PricingRecord record : report.getInvalidRecordsList()) {
                    if (record.getValidationError() != null && record.getValidationError().contains("Missing price")) {
                        reportContent.append(String.format("  - GUID: %s, Trade Date: %s, Exchange: %s, Product Type: %s\n",
                            record.getInstrumentGuid() != null && !record.getInstrumentGuid().trim().isEmpty() 
                                ? record.getInstrumentGuid() : "",
                            record.getTradeDate() != null ? record.getTradeDate().toString() : "",
                            record.getExchange() != null && !record.getExchange().trim().isEmpty() 
                                ? record.getExchange() : "",
                            record.getProductType() != null && !record.getProductType().trim().isEmpty() 
                                ? record.getProductType() : ""));
                    }
                }
                reportContent.append("\n");
            }
            
            // Missing Instrument GUID Records
            if (report.getMissingInstrumentGuidRecords() > 0) {
                reportContent.append("Missing Instrument GUID Records:\n");
                for (PricingRecord record : report.getInvalidRecordsList()) {
                    if (record.getValidationError() != null && record.getValidationError().contains("Missing instrument GUID")) {
                        reportContent.append(String.format("  - Trade Date: %s, Price: %s, Exchange: %s, Product Type: %s\n",
                            record.getTradeDate() != null ? record.getTradeDate().toString() : "",
                            record.getPrice() != null ? String.format("%.2f", record.getPrice()) : "",
                            record.getExchange() != null && !record.getExchange().trim().isEmpty() 
                                ? record.getExchange() : "",
                            record.getProductType() != null && !record.getProductType().trim().isEmpty() 
                                ? record.getProductType() : ""));
                    }
                }
                reportContent.append("\n");
            }
            
            // Missing Trade Date Records
            if (report.getMissingTradeDateRecords() > 0) {
                reportContent.append("Missing Trade Date Records:\n");
                for (PricingRecord record : report.getInvalidRecordsList()) {
                    if (record.getValidationError() != null && record.getValidationError().contains("Missing trade date")) {
                        reportContent.append(String.format("  - GUID: %s, Price: %s, Exchange: %s, Product Type: %s\n",
                            record.getInstrumentGuid() != null && !record.getInstrumentGuid().trim().isEmpty() 
                                ? record.getInstrumentGuid() : "",
                            record.getPrice() != null ? String.format("%.2f", record.getPrice()) : "",
                            record.getExchange() != null && !record.getExchange().trim().isEmpty() 
                                ? record.getExchange() : "",
                            record.getProductType() != null && !record.getProductType().trim().isEmpty() 
                                ? record.getProductType() : ""));
                    }
                }
                reportContent.append("\n");
            }
            
            // Missing Exchange Records
            if (report.getMissingExchangeRecords() > 0) {
                reportContent.append("Missing Exchange Records:\n");
                for (PricingRecord record : report.getInvalidRecordsList()) {
                    if (record.getValidationError() != null && record.getValidationError().contains("Missing exchange")) {
                        reportContent.append(String.format("  - GUID: %s, Trade Date: %s, Price: %s, Product Type: %s\n",
                            record.getInstrumentGuid() != null && !record.getInstrumentGuid().trim().isEmpty() 
                                ? record.getInstrumentGuid() : "",
                            record.getTradeDate() != null ? record.getTradeDate().toString() : "",
                            record.getPrice() != null ? String.format("%.2f", record.getPrice()) : "",
                            record.getProductType() != null && !record.getProductType().trim().isEmpty() 
                                ? record.getProductType() : ""));
                    }
                }
                reportContent.append("\n");
            }
            
            // Missing Product Type Records
            if (report.getMissingProductTypeRecords() > 0) {
                reportContent.append("Missing Product Type Records:\n");
                for (PricingRecord record : report.getInvalidRecordsList()) {
                    if (record.getValidationError() != null && record.getValidationError().contains("Missing product type")) {
                        reportContent.append(String.format("  - GUID: %s, Trade Date: %s, Price: %s, Exchange: %s\n",
                            record.getInstrumentGuid() != null && !record.getInstrumentGuid().trim().isEmpty() 
                                ? record.getInstrumentGuid() : "",
                            record.getTradeDate() != null ? record.getTradeDate().toString() : "",
                            record.getPrice() != null ? String.format("%.2f", record.getPrice()) : "",
                            record.getExchange() != null && !record.getExchange().trim().isEmpty() 
                                ? record.getExchange() : ""));
                    }
                }
                reportContent.append("\n");
            }
        }
        
        // All Records
        reportContent.append("ALL RECORDS\n");
        reportContent.append("-".repeat(80)).append("\n");
        reportContent.append(String.format("%-15s %-12s %-10s %-8s %-12s %-8s\n", 
            "Instrument GUID", "Trade Date", "Price", "Exchange", "Product Type", "Status"));
        reportContent.append("-".repeat(80)).append("\n");
        
        for (PricingRecord record : report.getAllRecords()) {
            // Format price: show invalid values, blank for missing
            String price;
            if (record.getPrice() != null) {
                price = String.format("%.2f", record.getPrice());
            } else {
                // Check if it's an invalid value (stored in validation error)
                String error = record.getValidationError();
                if (error != null && error.contains("Invalid price format: ")) {
                    // Extract original invalid value from error message
                    // Error might be: "Invalid price format: INVALID" or "Invalid price format: ABC123; Other error"
                    int startIndex = error.indexOf("Invalid price format: ") + "Invalid price format: ".length();
                    int endIndex = error.indexOf(";", startIndex);
                    if (endIndex == -1) {
                        // No other errors, take until end of string
                        price = error.substring(startIndex).trim();
                    } else {
                        // Other errors present, take until first semicolon
                        price = error.substring(startIndex, endIndex).trim();
                    }
                } else {
                    // Actually missing, show blank
                    price = "";
                }
            }
            
            // Format date: blank for missing
            String date = record.getTradeDate() != null ? record.getTradeDate().toString() : "";
            
            // Format exchange: blank for missing, show actual value for invalid
            String exchange = (record.getExchange() != null && !record.getExchange().trim().isEmpty()) 
                    ? record.getExchange() : "";
            
            // Format product type: blank for missing, show actual value for invalid
            String productType = (record.getProductType() != null && !record.getProductType().trim().isEmpty()) 
                    ? record.getProductType() : "";
            
            // Format GUID: blank for missing
            String guid = (record.getInstrumentGuid() != null && !record.getInstrumentGuid().trim().isEmpty()) 
                    ? record.getInstrumentGuid() : "";
            
            reportContent.append(String.format("%-15s %-12s %-10s %-8s %-12s %-8s\n",
                guid,
                date,
                price,
                exchange,
                productType,
                record.isValid() ? "VALID" : "INVALID"));
        }
        
        reportContent.append("\n");
        reportContent.append("=".repeat(80)).append("\n");
        reportContent.append("End of Report\n");
        reportContent.append("=".repeat(80)).append("\n");
        
        // Write to file
        try (FileWriter writer = new FileWriter(outputPath)) {
            writer.write(reportContent.toString());
        }
        
        logger.info("Report generated successfully: {}", outputPath);
        return reportContent.toString();
    }
}

