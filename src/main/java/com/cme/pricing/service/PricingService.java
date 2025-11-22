package com.cme.pricing.service;

import com.cme.pricing.model.PricingRecord;
import com.cme.pricing.model.ValidationReport;
import com.cme.pricing.parser.CSVParser;
import com.cme.pricing.validator.PricingValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service class for pricing data operations
 */
@Service
public class PricingService {
    
    private static final Logger logger = LoggerFactory.getLogger(PricingService.class);
    
    @Autowired
    private CSVParser csvParser;
    
    @Autowired
    private PricingValidator validator;
    
    private List<PricingRecord> records = new ArrayList<>();
    private ValidationReport currentReport;

    /**
     * Loads and validates pricing data from a CSV file
     */
    public ValidationReport loadAndValidateData(String filePath) throws IOException {
        logger.info("Loading data from file: {}", filePath);
        
        // Validate file format
        if (!filePath.toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException("Unsupported file format. Please use CSV format.");
        }
        
        // Parse CSV file
        records = csvParser.parseFile(filePath);
        
        // Validate all records
        validator.validateAllRecords(records);
        
        // Generate report
        currentReport = generateReport();
        
        logger.info("Data loaded and validated. Total records: {}, Valid: {}, Invalid: {}", 
                   currentReport.getTotalRecords(), 
                   currentReport.getValidRecords(), 
                   currentReport.getInvalidRecords());
        
        return currentReport;
    }

    /**
     * Generates a validation report
     */
    public ValidationReport generateReport() {
        ValidationReport report = new ValidationReport();
        
        report.setTotalRecords(records.size());
        
        // Sort records: null GUIDs stay at original position, non-null GUIDs sorted ascending
        List<PricingRecord> sortedRecords = sortRecordsForReport(new ArrayList<>(records));
        report.setAllRecords(sortedRecords);
        
        // Count valid and invalid records
        long validCount = records.stream().filter(PricingRecord::isValid).count();
        report.setValidRecords((int) validCount);
        report.setInvalidRecords(records.size() - (int) validCount);
        
        // Collect invalid records
        List<PricingRecord> invalidList = records.stream()
                .filter(r -> !r.isValid())
                .collect(Collectors.toList());
        report.setInvalidRecordsList(invalidList);
        
        // Count specific error types
        int missingPrice = 0;
        int invalidPriceFormat = 0;
        int negativePrice = 0;
        int zeroPrice = 0;
        int missingInstrumentGuid = 0;
        int missingTradeDate = 0;
        int missingExchange = 0;
        int missingProductType = 0;
        int invalidExchange = 0;
        int invalidProductType = 0;
        int duplicates = 0;
        List<String> duplicateInfo = new ArrayList<>();
        
        for (PricingRecord record : records) {
            if (!record.isValid()) {
                String error = record.getValidationError();
                if (error != null) {
                    if (error.contains("Missing price")) {
                        missingPrice++;
                    }
                    if (error.contains("Invalid price format")) {
                        invalidPriceFormat++;
                    }
                    if (error.contains("Negative price")) {
                        negativePrice++;
                    }
                    if (error.contains("Zero price")) {
                        zeroPrice++;
                    }
                    if (error.contains("Missing instrument GUID")) {
                        missingInstrumentGuid++;
                    }
                    if (error.contains("Missing trade date")) {
                        missingTradeDate++;
                    }
                    if (error.contains("Missing exchange")) {
                        missingExchange++;
                    }
                    if (error.contains("Missing product type")) {
                        missingProductType++;
                    }
                    if (error.contains("Invalid exchange")) {
                        invalidExchange++;
                    }
                    if (error.contains("Invalid product type")) {
                        invalidProductType++;
                    }
                    if (error.contains("Duplicate GUID") || error.contains("Duplicate record")) {
                        duplicates++;
                        duplicateInfo.add(record.getInstrumentGuid() + " - " + record.getTradeDate());
                    }
                }
            }
        }
        
        report.setMissingPriceRecords(missingPrice);
        report.setInvalidPriceFormatRecords(invalidPriceFormat);
        report.setNegativePriceRecords(negativePrice);
        report.setZeroPriceRecords(zeroPrice);
        report.setMissingInstrumentGuidRecords(missingInstrumentGuid);
        report.setMissingTradeDateRecords(missingTradeDate);
        report.setMissingExchangeRecords(missingExchange);
        report.setMissingProductTypeRecords(missingProductType);
        report.setInvalidExchangeRecords(invalidExchange);
        report.setInvalidProductTypeRecords(invalidProductType);
        report.setDuplicateRecords(duplicates);
        report.setDuplicateRecordsList(duplicateInfo);
        
        return report;
    }

    /**
     * Gets all pricing records
     */
    public List<PricingRecord> getAllRecords() {
        return new ArrayList<>(records);
    }
    
    /**
     * Gets all pricing records sorted for display (null GUIDs stay at original position, non-null GUIDs sorted ascending)
     */
    public List<PricingRecord> getAllRecordsSorted() {
        return sortRecordsForReport(new ArrayList<>(records));
    }

    /**
     * Gets a record by instrument GUID
     */
    public Optional<PricingRecord> getRecordByGuid(String instrumentGuid) {
        return records.stream()
                .filter(r -> r.getInstrumentGuid() != null && r.getInstrumentGuid().equals(instrumentGuid))
                .findFirst();
    }
    
    /**
     * Gets all records by instrument GUID
     */
    public List<PricingRecord> getAllRecordsByGuid(String instrumentGuid) {
        return records.stream()
                .filter(r -> r.getInstrumentGuid() != null && r.getInstrumentGuid().equals(instrumentGuid))
                .collect(Collectors.toList());
    }
    
    /**
     * Gets a record by index
     */
    public Optional<PricingRecord> getRecordByIndex(int index) {
        if (index >= 0 && index < records.size()) {
            return Optional.of(records.get(index));
        }
        return Optional.empty();
    }
    
    /**
     * Gets the index of a record in the list
     */
    public int getRecordIndex(PricingRecord record) {
        return records.indexOf(record);
    }

    /**
     * Updates a pricing record
     */
    public boolean updateRecord(String instrumentGuid, PricingRecord updatedRecord) {
        Optional<PricingRecord> existing = getRecordByGuid(instrumentGuid);
        
        if (existing.isPresent()) {
            // Validate price before updating - must be > 0
            if (updatedRecord.getPrice() != null && updatedRecord.getPrice() <= 0) {
                logger.warn("Cannot update record with price <= 0: {}", updatedRecord.getPrice());
                return false;
            }
            
            PricingRecord record = existing.get();
            
            // Only update fields that are provided (non-null)
            if (updatedRecord.getPrice() != null) {
                record.setPrice(updatedRecord.getPrice());
                record.setOriginalPriceValue(null); // Clear invalid price value
            }
            if (updatedRecord.getExchange() != null) {
                record.setExchange(updatedRecord.getExchange());
            }
            if (updatedRecord.getProductType() != null) {
                record.setProductType(updatedRecord.getProductType());
            }
            if (updatedRecord.getTradeDate() != null) {
                record.setTradeDate(updatedRecord.getTradeDate());
            }
            
            // Re-validate the updated record
            validator.validateRecord(record);
            
            // Re-check for duplicates
            validator.identifyDuplicates(records);
            
            // Regenerate report
            currentReport = generateReport();
            
            logger.info("Record {} updated successfully", instrumentGuid);
            return true;
        }
        
        logger.warn("Record {} not found for update", instrumentGuid);
        return false;
    }
    
    /**
     * Updates a pricing record by index
     */
    public boolean updateRecordByIndex(int index, PricingRecord updatedRecord) {
        Optional<PricingRecord> existing = getRecordByIndex(index);
        
        if (existing.isPresent()) {
            // Validate price before updating - must be > 0
            if (updatedRecord.getPrice() != null && updatedRecord.getPrice() <= 0) {
                logger.warn("Cannot update record with price <= 0: {}", updatedRecord.getPrice());
                return false;
            }
            
            PricingRecord record = existing.get();
            
            // Only update fields that are provided (non-null)
            if (updatedRecord.getPrice() != null) {
                record.setPrice(updatedRecord.getPrice());
                record.setOriginalPriceValue(null); // Clear invalid price value
            }
            if (updatedRecord.getExchange() != null) {
                record.setExchange(updatedRecord.getExchange());
            }
            if (updatedRecord.getProductType() != null) {
                record.setProductType(updatedRecord.getProductType());
            }
            if (updatedRecord.getTradeDate() != null) {
                record.setTradeDate(updatedRecord.getTradeDate());
            }
            
            // Re-validate the updated record
            validator.validateRecord(record);
            
            // Re-check for duplicates
            validator.identifyDuplicates(records);
            
            // Regenerate report
            currentReport = generateReport();
            
            logger.info("Record at index {} updated successfully", index);
            return true;
        }
        
        logger.warn("Record at index {} not found for update", index);
        return false;
    }

    /**
     * Deletes a pricing record
     */
    public boolean deleteRecord(String instrumentGuid) {
        boolean removed = records.removeIf(r -> r.getInstrumentGuid().equals(instrumentGuid));
        
        if (removed) {
            // Regenerate report
            currentReport = generateReport();
            logger.info("Record {} deleted successfully", instrumentGuid);
        } else {
            logger.warn("Record {} not found for deletion", instrumentGuid);
        }
        
        return removed;
    }

    /**
     * Corrects an invalid record by GUID
     */
    public boolean correctRecord(String instrumentGuid, PricingRecord correction) {
        Optional<PricingRecord> existing = getRecordByGuid(instrumentGuid);
        
        if (existing.isPresent()) {
            // Validate price before correcting - must be > 0
            if (correction.getPrice() != null && correction.getPrice() <= 0) {
                logger.warn("Cannot correct record with price <= 0: {}", correction.getPrice());
                return false;
            }
            
            PricingRecord record = existing.get();
            String oldGuid = record.getInstrumentGuid();
            
            // Apply corrections
            if (correction.getInstrumentGuid() != null && !correction.getInstrumentGuid().trim().isEmpty()) {
                String newGuid = correction.getInstrumentGuid().trim();
                // Check if new GUID already exists (excluding current record)
                boolean guidExists = records.stream()
                    .anyMatch(r -> r != record && 
                                  r.getInstrumentGuid() != null && 
                                  r.getInstrumentGuid().equals(newGuid));
                if (guidExists) {
                    logger.warn("Cannot assign GUID {} - already exists in another record", newGuid);
                    return false;
                }
                record.setInstrumentGuid(newGuid);
                logger.info("GUID corrected: {} -> {}", oldGuid, newGuid);
            }
            if (correction.getPrice() != null) {
                record.setPrice(correction.getPrice());
                record.setOriginalPriceValue(null); // Clear invalid price value
            }
            if (correction.getExchange() != null) {
                record.setExchange(correction.getExchange());
            }
            if (correction.getProductType() != null) {
                record.setProductType(correction.getProductType());
            }
            if (correction.getTradeDate() != null) {
                record.setTradeDate(correction.getTradeDate());
            }
            
            // Re-validate
            validator.validateRecord(record);
            validator.identifyDuplicates(records);
            
            // Regenerate report
            currentReport = generateReport();
            
            logger.info("Record {} corrected successfully", instrumentGuid);
            return true;
        }
        
        logger.warn("Record {} not found for correction", instrumentGuid);
        return false;
    }
    
    /**
     * Corrects an invalid record by index
     */
    public boolean correctRecordByIndex(int index, PricingRecord correction) {
        Optional<PricingRecord> existing = getRecordByIndex(index);
        
        if (existing.isPresent()) {
            // Validate price before correcting - must be > 0
            if (correction.getPrice() != null && correction.getPrice() <= 0) {
                logger.warn("Cannot correct record with price <= 0: {}", correction.getPrice());
                return false;
            }
            
            PricingRecord record = existing.get();
            String oldGuid = record.getInstrumentGuid();
            
            // Apply corrections
            if (correction.getInstrumentGuid() != null && !correction.getInstrumentGuid().trim().isEmpty()) {
                String newGuid = correction.getInstrumentGuid().trim();
                // Check if new GUID already exists (excluding current record)
                boolean guidExists = records.stream()
                    .anyMatch(r -> r != record && 
                                  r.getInstrumentGuid() != null && 
                                  r.getInstrumentGuid().equals(newGuid));
                if (guidExists) {
                    logger.warn("Cannot assign GUID {} - already exists in another record", newGuid);
                    return false;
                }
                record.setInstrumentGuid(newGuid);
                logger.info("GUID corrected: {} -> {}", oldGuid != null ? oldGuid : "(empty)", newGuid);
            }
            if (correction.getPrice() != null) {
                record.setPrice(correction.getPrice());
                record.setOriginalPriceValue(null); // Clear invalid price value
            }
            if (correction.getExchange() != null) {
                record.setExchange(correction.getExchange());
            }
            if (correction.getProductType() != null) {
                record.setProductType(correction.getProductType());
            }
            if (correction.getTradeDate() != null) {
                record.setTradeDate(correction.getTradeDate());
            }
            
            // Re-validate
            validator.validateRecord(record);
            validator.identifyDuplicates(records);
            
            // Regenerate report
            currentReport = generateReport();
            
            logger.info("Record at index {} corrected successfully", index);
            return true;
        }
        
        logger.warn("Record at index {} not found for correction", index);
        return false;
    }
    
    /**
     * Deletes a record by index
     */
    public boolean deleteRecordByIndex(int index) {
        if (index >= 0 && index < records.size()) {
            records.remove(index);
            
            // Regenerate report
            currentReport = generateReport();
            logger.info("Record at index {} deleted successfully", index);
            return true;
        }
        
        logger.warn("Record at index {} not found for deletion", index);
        return false;
    }

    /**
     * Creates a new pricing record
     */
    public boolean createRecord(PricingRecord newRecord) {
        try {
            // Validate price before creating - must be > 0
            if (newRecord.getPrice() != null && newRecord.getPrice() <= 0) {
                logger.warn("Cannot create record with price <= 0: {}", newRecord.getPrice());
                return false;
            }
            
            // Add the record to the list
            records.add(newRecord);
            
            // Validate the new record
            validator.validateRecord(newRecord);
            
            // Check for duplicates (this will mark duplicates as invalid)
            validator.identifyDuplicates(records);
            
            // Regenerate report
            currentReport = generateReport();
            
            logger.info("Record {} created successfully", newRecord.getInstrumentGuid());
            return true;
        } catch (Exception e) {
            logger.error("Error creating record: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Gets the current validation report
     */
    public ValidationReport getCurrentReport() {
        if (currentReport == null) {
            currentReport = generateReport();
        }
        return currentReport;
    }
    
    /**
     * Sorts records for report: null GUIDs stay at original position, non-null GUIDs sorted ascending
     */
    private List<PricingRecord> sortRecordsForReport(List<PricingRecord> recordsToSort) {
        // Separate records with null GUIDs and non-null GUIDs
        List<PricingRecord> nullGuidRecords = new ArrayList<>();
        List<PricingRecord> nonNullGuidRecords = new ArrayList<>();
        List<Integer> nullGuidOriginalIndices = new ArrayList<>();
        
        for (int i = 0; i < recordsToSort.size(); i++) {
            PricingRecord record = recordsToSort.get(i);
            String guid = record.getInstrumentGuid();
            if (guid == null || guid.trim().isEmpty()) {
                nullGuidRecords.add(record);
                nullGuidOriginalIndices.add(i);
            } else {
                nonNullGuidRecords.add(record);
            }
        }
        
        // Sort non-null GUID records in ascending order
        // Numeric GUIDs sorted numerically, non-numeric GUIDs sorted alphabetically after numeric ones
        nonNullGuidRecords.sort((r1, r2) -> {
            String guid1 = r1.getInstrumentGuid();
            String guid2 = r2.getInstrumentGuid();
            
            // Try to parse both as integers for numeric comparison
            try {
                int num1 = Integer.parseInt(guid1);
                int num2 = Integer.parseInt(guid2);
                return Integer.compare(num1, num2);
            } catch (NumberFormatException e1) {
                // At least one is not numeric
                try {
                    // If guid1 is numeric but guid2 is not, guid1 comes first
                    Integer.parseInt(guid1);
                    return -1;
                } catch (NumberFormatException e2) {
                    try {
                        // If guid2 is numeric but guid1 is not, guid2 comes first
                        Integer.parseInt(guid2);
                        return 1;
                    } catch (NumberFormatException e3) {
                        // Both are non-numeric, use string comparison
                        return guid1.compareTo(guid2);
                    }
                }
            }
        });
        
        // Reconstruct list: place null GUIDs at their original positions, fill rest with sorted non-null GUIDs
        List<PricingRecord> sortedList = new ArrayList<>(recordsToSort.size());
        for (int i = 0; i < recordsToSort.size(); i++) {
            sortedList.add(null); // Initialize with null
        }
        
        // Place null GUID records at their original positions
        for (int i = 0; i < nullGuidRecords.size(); i++) {
            int originalIndex = nullGuidOriginalIndices.get(i);
            sortedList.set(originalIndex, nullGuidRecords.get(i));
        }
        
        // Fill remaining positions with sorted non-null GUID records
        int nonNullIndex = 0;
        for (int i = 0; i < sortedList.size(); i++) {
            if (sortedList.get(i) == null) {
                sortedList.set(i, nonNullGuidRecords.get(nonNullIndex));
                nonNullIndex++;
            }
        }
        
        return sortedList;
    }
}

