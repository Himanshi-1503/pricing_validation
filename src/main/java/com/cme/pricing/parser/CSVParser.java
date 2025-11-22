package com.cme.pricing.parser;

import com.cme.pricing.model.PricingRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

/**
 * Parser for CSV format pricing data
 */
@Component
public class CSVParser {

    private static final Logger logger = LoggerFactory.getLogger(CSVParser.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Parses a CSV file and returns a list of PricingRecord objects
     */
    public List<PricingRecord> parseFile(String filePath) throws IOException {
        List<PricingRecord> records = new ArrayList<>();

        logger.info("Parsing CSV file: {}", filePath);
        
        // Handle relative paths - if path doesn't start with /, make it relative to working directory
        java.io.File file = new java.io.File(filePath);
        if (!file.isAbsolute() && !file.exists()) {
            // Try with /app prefix (Docker working directory)
            java.io.File dockerFile = new java.io.File("/app", filePath);
            if (dockerFile.exists()) {
                filePath = dockerFile.getAbsolutePath();
                logger.info("Found file in Docker path: {}", filePath);
            }
        }

        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            // Read header row
            String[] header = reader.readNext();
            if (header == null) {
                logger.warn("CSV file is empty or has no header");
                return records;
            }

            // Read data rows
            String[] line;
            int lineNumber = 1; // Start from 1 since header is line 0

            while ((line = reader.readNext()) != null) {
                lineNumber++;
                try {
                    PricingRecord record = parseLine(line, lineNumber);
                    if (record != null) {
                        records.add(record);
                    }
                } catch (Exception e) {
                    logger.error("Error parsing line {}: {}", lineNumber, e.getMessage());
                }
            }
        } catch (CsvException e) {
            logger.error("CSV parsing error: {}", e.getMessage());
            throw new IOException("Failed to parse CSV file", e);
        }

        logger.info("Successfully parsed {} records from CSV file", records.size());
        return records;
    }

    /**
     * Parses a single CSV line into a PricingRecord
     */
    private PricingRecord parseLine(String[] line, int lineNumber) {
        if (line.length < 5) {
            logger.warn("Line {} has insufficient columns (expected 5, found {})", lineNumber, line.length);
            return null;
        }

        PricingRecord record = new PricingRecord();

        try {
            // instrument_guid
            record.setInstrumentGuid(line[0].trim());

            // trade_date
            try {
                record.setTradeDate(LocalDate.parse(line[1].trim(), DATE_FORMATTER));
            } catch (DateTimeParseException e) {
                logger.warn("Invalid date format on line {}: {}", lineNumber, line[1]);
                record.setTradeDate(null);
            }

            // price - handle missing or invalid values
            String priceStr = line[2].trim();
            if (priceStr.isEmpty()) {
                // Actually missing - will be marked as "Missing price"
                record.setPrice(null);
                record.setOriginalPriceValue(null);
            } else {
                try {
                    record.setPrice(Double.parseDouble(priceStr));
                    record.setOriginalPriceValue(null); // Valid price, no need to store original
                } catch (NumberFormatException e) {
                    logger.warn("Invalid price format on line {}: {}", lineNumber, priceStr);
                    record.setPrice(null);
                    // Store original invalid value for display
                    record.setOriginalPriceValue(priceStr);
                    // Mark as invalid format, not missing
                    record.setValidationError("Invalid price format: " + priceStr);
                }
            }

            // exchange
            record.setExchange(line[3].trim());

            // product_type
            record.setProductType(line[4].trim());

        } catch (Exception e) {
            logger.error("Error parsing line {}: {}", lineNumber, e.getMessage());
            return null;
        }

        return record;
    }
}
