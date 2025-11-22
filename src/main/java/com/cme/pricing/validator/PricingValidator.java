package com.cme.pricing.validator;

import com.cme.pricing.model.PricingRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validator class for pricing records
 */
@Component
public class PricingValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(PricingValidator.class);
    
    // Valid exchanges
    private static final Set<String> VALID_EXCHANGES = Set.of("CME", "NYMEX", "CBOT", "COMEX");
    
    // Valid product types
    private static final Set<String> VALID_PRODUCT_TYPES = Set.of("FUT", "OPT");

    /**
     * Validates a single pricing record
     */
    public void validateRecord(PricingRecord record) {
        record.setValid(true);
        
        // Preserve parser's error message if it's about invalid format (only if price is still null)
        String parserError = record.getValidationError();
        boolean isInvalidFormat = (parserError != null && parserError.contains("Invalid price format"));
        
        // Clear error at the start - will be set again if validation fails
            record.setValidationError(null);
        
        List<String> errors = new java.util.ArrayList<>();
        
        // Check for missing price
        if (record.getPrice() == null) {
            // Check if it's already marked as invalid format by parser
            if (isInvalidFormat) {
                // Already marked as invalid format, preserve it
                errors.add(parserError);
            } else {
                // Actually missing
                errors.add("Missing price value");
            }
            record.setValid(false);
        } else if (record.getPrice() < 0) {
            errors.add("Negative price");
            record.setValid(false);
        } else if (record.getPrice() == 0) {
            errors.add("Zero price");
            record.setValid(false);
        }
        
        // Validate exchange
        if (record.getExchange() == null || record.getExchange().trim().isEmpty()) {
            errors.add("Missing exchange");
            record.setValid(false);
        } else if (!VALID_EXCHANGES.contains(record.getExchange().toUpperCase())) {
            errors.add("Invalid exchange: " + record.getExchange());
            record.setValid(false);
        }
        
        // Validate product type
        if (record.getProductType() == null || record.getProductType().trim().isEmpty()) {
            errors.add("Missing product type");
            record.setValid(false);
        } else if (!VALID_PRODUCT_TYPES.contains(record.getProductType().toUpperCase())) {
            errors.add("Invalid product type: " + record.getProductType());
            record.setValid(false);
        }
        
        // Validate instrument GUID (primary key - must be present and unique)
        if (record.getInstrumentGuid() == null || record.getInstrumentGuid().trim().isEmpty()) {
            errors.add("Missing instrument GUID (primary key required)");
            record.setValid(false);
        }
        
        // Validate trade date
        if (record.getTradeDate() == null) {
            errors.add("Missing trade date");
            record.setValid(false);
        }
        
        if (!errors.isEmpty()) {
            record.setValidationError(String.join("; ", errors));
            logger.warn("Validation failed for record {}: {}", record.getInstrumentGuid(), record.getValidationError());
        }
    }

    /**
     * Identifies duplicate GUIDs in a list (GUID is primary key - must be unique)
     * First occurrence is valid, subsequent occurrences are invalid
     */
    public void identifyDuplicates(List<PricingRecord> records) {
        Set<String> seenGuids = new HashSet<>();
        
        for (PricingRecord record : records) {
            String guid = record.getInstrumentGuid();
            
            // Skip records with null/empty GUID (already marked invalid by validateRecord)
            if (guid == null || guid.trim().isEmpty()) {
                continue;
            }
            
            // Normalize GUID for comparison (trim and case-insensitive if needed)
            String normalizedGuid = guid.trim();
            
            // If this GUID has been seen before, mark this record as invalid
            if (seenGuids.contains(normalizedGuid)) {
                record.setValid(false);
                String currentError = record.getValidationError();
                // Only add "Duplicate GUID" if it's not already in the error message
                boolean isNewDuplicate = (currentError == null || !currentError.contains("Duplicate GUID"));
                if (isNewDuplicate) {
                    if (currentError == null || currentError.trim().isEmpty()) {
                        record.setValidationError("Duplicate GUID (primary key violation)");
                } else {
                        record.setValidationError(currentError + "; Duplicate GUID (primary key violation)");
                    }
                    // Only log if this is a newly discovered duplicate
                    logger.warn("Duplicate GUID found: {} - marking as invalid", normalizedGuid);
                }
            } else {
                // First occurrence of this GUID - add to seen set
                // Note: Record may still be invalid due to other validation errors
                seenGuids.add(normalizedGuid);
            }
        }
    }

    /**
     * Validates all records and identifies duplicates
     */
    public void validateAllRecords(List<PricingRecord> records) {
        logger.info("Starting validation of {} records", records.size());
        
        // First, validate each record individually
        for (PricingRecord record : records) {
            validateRecord(record);
        }
        
        // Then, identify duplicates
        identifyDuplicates(records);
        
        logger.info("Validation completed");
    }
}

