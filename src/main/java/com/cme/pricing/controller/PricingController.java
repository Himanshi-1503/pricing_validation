package com.cme.pricing.controller;

import com.cme.pricing.model.PricingRecord;
import com.cme.pricing.model.ValidationReport;
import com.cme.pricing.report.ReportGenerator;
import com.cme.pricing.service.PricingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for pricing data operations
 */
@RestController
@RequestMapping("/api/pricing")
public class PricingController {
    
    private static final Logger logger = LoggerFactory.getLogger(PricingController.class);
    
    @Autowired
    private PricingService pricingService;
    
    @Autowired
    private ReportGenerator reportGenerator;

    /**
     * Helper method to format price value - shows invalid values (like "INVALID") but blank for null/missing
     */
    private Object formatPriceValue(PricingRecord record) {
        if (record.getPrice() != null) {
            return record.getPrice();
        }
        // First check originalPriceValue (stored by parser for invalid formats)
        if (record.getOriginalPriceValue() != null && !record.getOriginalPriceValue().trim().isEmpty()) {
            return record.getOriginalPriceValue();
        }
        // Fallback: extract from validation error message
        String error = record.getValidationError();
        if (error != null && error.contains("Invalid price format: ")) {
            int startIndex = error.indexOf("Invalid price format: ") + "Invalid price format: ".length();
            int endIndex = error.indexOf(";", startIndex);
            if (endIndex == -1) {
                return error.substring(startIndex).trim();
            }
            return error.substring(startIndex, endIndex).trim();
        }
        // Actually missing/null - return empty string
        return "";
    }

    /**
     * Root endpoint - provides API information
     * GET /
     */
    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> getApiInfo() {
        Map<String, Object> info = new java.util.HashMap<>();
        info.put("application", "Pricing Data Validation & Reporting Utility");
        info.put("version", "1.0.0");
        info.put("status", "running");
        info.put("endpoints", Map.of(
            "load", "POST /api/pricing/load",
            "report", "GET /api/pricing/report",
            "generateReport", "POST /api/pricing/report/generate",
            "allRecords", "GET /api/pricing/records",
                "getSpecificRecord", "GET /api/pricing/records/{instrumentGuid}",
            "updateRecord", "PUT /api/pricing/records/{instrumentGuid}",
            "deleteRecord", "DELETE /api/pricing/records/{instrumentGuid}",
                "updateSpecificRecord", "POST /api/pricing/records/{instrumentGuid}/correct"));
        return ResponseEntity.ok(info);
    }

    /**
     * Load and validate pricing data from a file
     * POST /api/pricing/load
     */
    @PostMapping("/load")
    public ResponseEntity<?> loadData(@RequestBody Map<String, String> request) {
        try {
            String filePath = request.get("filePath");
            if (filePath == null || filePath.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "filePath is required"));
            }
            
            ValidationReport report = pricingService.loadAndValidateData(filePath);

            // Return brief summary matching CLI format
            int totalMissing = report.getMissingPriceRecords() +
                    report.getMissingInstrumentGuidRecords() +
                    report.getMissingTradeDateRecords() +
                    report.getMissingExchangeRecords() +
                    report.getMissingProductTypeRecords();

            // Use LinkedHashMap to maintain insertion order
            Map<String, Object> response = new java.util.LinkedHashMap<>();
            response.put("message", "Data loaded and validated successfully!");
            response.put("totalRecords", report.getTotalRecords());
            response.put("validRecords", report.getValidRecords());
            response.put("invalidRecords", report.getInvalidRecords());
            if (report.getDuplicateRecords() > 0) {
                response.put("duplicateRecords", report.getDuplicateRecords());
            }
            response.put("missingValues", totalMissing);

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            logger.error("Error loading data: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error loading file: " + e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get validation report
     * GET /api/pricing/report
     */
    @GetMapping("/report")
    public ResponseEntity<?> getReport() {
        ValidationReport report = pricingService.getCurrentReport();

        if (report.getTotalRecords() == 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No data loaded. Please load a CSV file first using POST /api/pricing/load"));
        }

        // Calculate total missing values
        int totalMissing = report.getMissingPriceRecords() +
                report.getMissingInstrumentGuidRecords() +
                report.getMissingTradeDateRecords() +
                report.getMissingExchangeRecords() +
                report.getMissingProductTypeRecords();

        // Build error breakdown
        Map<String, Integer> errorBreakdown = new java.util.LinkedHashMap<>();
        if (report.getMissingPriceRecords() > 0) {
            errorBreakdown.put("Missing Price", report.getMissingPriceRecords());
        }
        if (report.getInvalidPriceFormatRecords() > 0) {
            errorBreakdown.put("Invalid Price Format", report.getInvalidPriceFormatRecords());
        }
        if (report.getMissingInstrumentGuidRecords() > 0) {
            errorBreakdown.put("Missing instrument_guid", report.getMissingInstrumentGuidRecords());
        }
        if (report.getMissingTradeDateRecords() > 0) {
            errorBreakdown.put("Missing trade_date", report.getMissingTradeDateRecords());
        }
        if (report.getMissingExchangeRecords() > 0) {
            errorBreakdown.put("Missing exchange", report.getMissingExchangeRecords());
        }
        if (report.getMissingProductTypeRecords() > 0) {
            errorBreakdown.put("Missing product_type", report.getMissingProductTypeRecords());
        }
        if (report.getInvalidExchangeRecords() > 0) {
            errorBreakdown.put("Invalid exchange", report.getInvalidExchangeRecords());
        }
        if (report.getInvalidProductTypeRecords() > 0) {
            errorBreakdown.put("Invalid product_type", report.getInvalidProductTypeRecords());
        }
        if (report.getDuplicateRecords() > 0) {
            errorBreakdown.put("Duplicate Records", report.getDuplicateRecords());
        }

        // Build invalid records list with full details
        List<Map<String, Object>> invalidRecordsList = new java.util.ArrayList<>();
        for (PricingRecord record : report.getInvalidRecordsList()) {
            Map<String, Object> recordInfo = new java.util.LinkedHashMap<>();
            recordInfo.put("guid",
                    record.getInstrumentGuid() != null && !record.getInstrumentGuid().trim().isEmpty()
                            ? record.getInstrumentGuid()
                            : "");
            recordInfo.put("tradeDate", record.getTradeDate() != null ? record.getTradeDate().toString() : "");
            recordInfo.put("price", formatPriceValue(record));
            recordInfo.put("exchange",
                    record.getExchange() != null && !record.getExchange().trim().isEmpty() ? record.getExchange() : "");
            recordInfo.put("productType",
                    record.getProductType() != null && !record.getProductType().trim().isEmpty()
                            ? record.getProductType()
                            : "");
            recordInfo.put("error", record.getValidationError() != null ? record.getValidationError() : "");
            invalidRecordsList.add(recordInfo);
        }

        // Build duplicate records list with full details
        List<Map<String, Object>> duplicateRecordsList = new java.util.ArrayList<>();
        for (PricingRecord record : report.getInvalidRecordsList()) {
            if (record.getValidationError() != null && record.getValidationError().contains("Duplicate")) {
                Map<String, Object> dupInfo = new java.util.LinkedHashMap<>();
                dupInfo.put("guid",
                        record.getInstrumentGuid() != null && !record.getInstrumentGuid().trim().isEmpty()
                                ? record.getInstrumentGuid()
                                : "");
                dupInfo.put("tradeDate", record.getTradeDate() != null ? record.getTradeDate().toString() : "");
                dupInfo.put("price", formatPriceValue(record));
                dupInfo.put("exchange",
                        record.getExchange() != null && !record.getExchange().trim().isEmpty() ? record.getExchange()
                                : "");
                dupInfo.put("productType",
                        record.getProductType() != null && !record.getProductType().trim().isEmpty()
                                ? record.getProductType()
                                : "");
                dupInfo.put("error", record.getValidationError() != null ? record.getValidationError() : "");
                duplicateRecordsList.add(dupInfo);
            }
        }

        // Build missing values details with full record information
        Map<String, List<Map<String, Object>>> missingValuesDetails = new java.util.LinkedHashMap<>();

        // Missing price records
        if (report.getMissingPriceRecords() > 0) {
            List<Map<String, Object>> missingPriceList = new java.util.ArrayList<>();
            for (PricingRecord record : report.getInvalidRecordsList()) {
                if (record.getValidationError() != null && record.getValidationError().contains("Missing price")) {
                    Map<String, Object> missingInfo = new java.util.LinkedHashMap<>();
                    missingInfo.put("guid",
                            record.getInstrumentGuid() != null && !record.getInstrumentGuid().trim().isEmpty()
                                    ? record.getInstrumentGuid()
                                    : "");
                    missingInfo.put("tradeDate", record.getTradeDate() != null ? record.getTradeDate().toString() : "");
                    missingInfo.put("price", ""); // Missing
                    missingInfo.put("exchange",
                            record.getExchange() != null && !record.getExchange().trim().isEmpty()
                                    ? record.getExchange()
                                    : "");
                    missingInfo.put("productType",
                            record.getProductType() != null && !record.getProductType().trim().isEmpty()
                                    ? record.getProductType()
                                    : "");
                    missingInfo.put("error", record.getValidationError() != null ? record.getValidationError() : "");
                    missingPriceList.add(missingInfo);
                }
            }
            if (!missingPriceList.isEmpty()) {
                missingValuesDetails.put("missingPrice", missingPriceList);
            }
        }

        // Missing instrument GUID records
        if (report.getMissingInstrumentGuidRecords() > 0) {
            List<Map<String, Object>> missingGuidList = new java.util.ArrayList<>();
            for (PricingRecord record : report.getInvalidRecordsList()) {
                if (record.getValidationError() != null
                        && record.getValidationError().contains("Missing instrument GUID")) {
                    Map<String, Object> missingInfo = new java.util.LinkedHashMap<>();
                    missingInfo.put("guid", ""); // Missing
                    missingInfo.put("tradeDate", record.getTradeDate() != null ? record.getTradeDate().toString() : "");
                    missingInfo.put("price", record.getPrice() != null ? record.getPrice() : "");
                    missingInfo.put("exchange",
                            record.getExchange() != null && !record.getExchange().trim().isEmpty()
                                    ? record.getExchange()
                                    : "");
                    missingInfo.put("productType",
                            record.getProductType() != null && !record.getProductType().trim().isEmpty()
                                    ? record.getProductType()
                                    : "");
                    missingInfo.put("error", record.getValidationError() != null ? record.getValidationError() : "");
                    missingGuidList.add(missingInfo);
                }
            }
            if (!missingGuidList.isEmpty()) {
                missingValuesDetails.put("missingInstrumentGuid", missingGuidList);
            }
        }

        // Missing trade date records
        if (report.getMissingTradeDateRecords() > 0) {
            List<Map<String, Object>> missingDateList = new java.util.ArrayList<>();
            for (PricingRecord record : report.getInvalidRecordsList()) {
                if (record.getValidationError() != null && record.getValidationError().contains("Missing trade date")) {
                    Map<String, Object> missingInfo = new java.util.LinkedHashMap<>();
                    missingInfo.put("guid",
                            record.getInstrumentGuid() != null && !record.getInstrumentGuid().trim().isEmpty()
                                    ? record.getInstrumentGuid()
                                    : "");
                    missingInfo.put("tradeDate", ""); // Missing
                    missingInfo.put("price", record.getPrice() != null ? record.getPrice() : "");
                    missingInfo.put("exchange",
                            record.getExchange() != null && !record.getExchange().trim().isEmpty()
                                    ? record.getExchange()
                                    : "");
                    missingInfo.put("productType",
                            record.getProductType() != null && !record.getProductType().trim().isEmpty()
                                    ? record.getProductType()
                                    : "");
                    missingInfo.put("error", record.getValidationError() != null ? record.getValidationError() : "");
                    missingDateList.add(missingInfo);
                }
            }
            if (!missingDateList.isEmpty()) {
                missingValuesDetails.put("missingTradeDate", missingDateList);
            }
        }

        // Missing exchange records
        if (report.getMissingExchangeRecords() > 0) {
            List<Map<String, Object>> missingExchangeList = new java.util.ArrayList<>();
            for (PricingRecord record : report.getInvalidRecordsList()) {
                if (record.getValidationError() != null && record.getValidationError().contains("Missing exchange")) {
                    Map<String, Object> missingInfo = new java.util.LinkedHashMap<>();
                    missingInfo.put("guid",
                            record.getInstrumentGuid() != null && !record.getInstrumentGuid().trim().isEmpty()
                                    ? record.getInstrumentGuid()
                                    : "");
                    missingInfo.put("tradeDate", record.getTradeDate() != null ? record.getTradeDate().toString() : "");
                    missingInfo.put("price", record.getPrice() != null ? record.getPrice() : "");
                    missingInfo.put("exchange", ""); // Missing
                    missingInfo.put("productType",
                            record.getProductType() != null && !record.getProductType().trim().isEmpty()
                                    ? record.getProductType()
                                    : "");
                    missingInfo.put("error", record.getValidationError() != null ? record.getValidationError() : "");
                    missingExchangeList.add(missingInfo);
                }
            }
            if (!missingExchangeList.isEmpty()) {
                missingValuesDetails.put("missingExchange", missingExchangeList);
            }
        }

        // Missing product type records
        if (report.getMissingProductTypeRecords() > 0) {
            List<Map<String, Object>> missingProductTypeList = new java.util.ArrayList<>();
            for (PricingRecord record : report.getInvalidRecordsList()) {
                if (record.getValidationError() != null
                        && record.getValidationError().contains("Missing product type")) {
                    Map<String, Object> missingInfo = new java.util.LinkedHashMap<>();
                    missingInfo.put("guid",
                            record.getInstrumentGuid() != null && !record.getInstrumentGuid().trim().isEmpty()
                                    ? record.getInstrumentGuid()
                                    : "");
                    missingInfo.put("tradeDate", record.getTradeDate() != null ? record.getTradeDate().toString() : "");
                    missingInfo.put("price", record.getPrice() != null ? record.getPrice() : "");
                    missingInfo.put("exchange",
                            record.getExchange() != null && !record.getExchange().trim().isEmpty()
                                    ? record.getExchange()
                                    : "");
                    missingInfo.put("productType", ""); // Missing
                    missingInfo.put("error", record.getValidationError() != null ? record.getValidationError() : "");
                    missingProductTypeList.add(missingInfo);
                }
            }
            if (!missingProductTypeList.isEmpty()) {
                missingValuesDetails.put("missingProductType", missingProductTypeList);
            }
        }

        // Build all records table (matching CLI format)
        List<Map<String, Object>> allRecordsTable = new java.util.ArrayList<>();
        for (PricingRecord record : report.getAllRecords()) {
            Map<String, Object> recordRow = new java.util.LinkedHashMap<>();

            recordRow.put("guid",
                    record.getInstrumentGuid() != null && !record.getInstrumentGuid().trim().isEmpty()
                            ? record.getInstrumentGuid()
                            : "");
            recordRow.put("date", record.getTradeDate() != null ? record.getTradeDate().toString() : "");
            recordRow.put("price", formatPriceValue(record));
            recordRow.put("exchange",
                    record.getExchange() != null && !record.getExchange().trim().isEmpty() ? record.getExchange() : "");
            recordRow.put("product",
                    record.getProductType() != null && !record.getProductType().trim().isEmpty()
                            ? record.getProductType()
                            : "");
            recordRow.put("status", record.isValid() ? "VALID" : "INVALID");

            allRecordsTable.add(recordRow);
        }

        // Build response matching CLI format
        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("totalRecords", report.getTotalRecords());
        response.put("validRecords", report.getValidRecords());
        response.put("invalidRecords", report.getInvalidRecords());
        response.put("duplicateRecords", report.getDuplicateRecords());
        response.put("missingValues", totalMissing);
        response.put("errorBreakdown", errorBreakdown);
        response.put("invalidRecordsList", invalidRecordsList);
        if (!duplicateRecordsList.isEmpty()) {
            response.put("duplicateRecordsList", duplicateRecordsList);
        }
        if (!missingValuesDetails.isEmpty()) {
            response.put("missingValuesDetails", missingValuesDetails);
        }
        response.put("allRecordsTable", allRecordsTable);

        return ResponseEntity.ok(response);
    }

    /**
     * Generate and save report to file
     * POST /api/pricing/report/generate
     */
    @PostMapping("/report/generate")
    public ResponseEntity<?> generateReport(@RequestBody Map<String, String> request) {
        try {
            // Check if data has been loaded
            List<PricingRecord> allRecords = pricingService.getAllRecords();
            if (allRecords.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error",
                                "No data loaded. Please load a CSV file first using POST /api/pricing/load"));
            }

            String outputPath = request.getOrDefault("outputPath", "validation_report.txt");
            // Get current report (will be regenerated if needed)
            ValidationReport report = pricingService.getCurrentReport();
            // Ensure report is up-to-date by regenerating it
            report = pricingService.generateReport();
            reportGenerator.generateTextReport(report, outputPath);

            // Calculate total missing values
            int totalMissing = report.getMissingPriceRecords() +
                    report.getMissingInstrumentGuidRecords() +
                    report.getMissingTradeDateRecords() +
                    report.getMissingExchangeRecords() +
                    report.getMissingProductTypeRecords();

            // Return response matching CLI format with summary
            Map<String, Object> response = new java.util.LinkedHashMap<>();
            response.put("message", "Report generated successfully: " + outputPath);
            response.put("outputPath", outputPath);
            Map<String, Object> summary = new java.util.LinkedHashMap<>();
            summary.put("totalRecords", report.getTotalRecords());
            summary.put("validRecords", report.getValidRecords());
            summary.put("invalidRecords", report.getInvalidRecords());
            summary.put("duplicateRecords", report.getDuplicateRecords());
            summary.put("missingValues", totalMissing);
            response.put("summary", summary);

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            logger.error("Error generating report: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to generate report: " + e.getMessage()));
        }
    }

    /**
     * Get all pricing records
     * GET /api/pricing/records
     */
    @GetMapping("/records")
    public ResponseEntity<?> getAllRecords() {
        List<PricingRecord> records = pricingService.getAllRecordsSorted();

        if (records.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                            "No records found. Please load a CSV file first using POST /api/pricing/load"));
        }

        // Format records matching CLI format with index information
        List<Map<String, Object>> formattedRecords = new java.util.ArrayList<>();
        
        for (int i = 0; i < records.size(); i++) {
            PricingRecord record = records.get(i);
            // Find the actual index in the unsorted list (original position)
            int actualIndex = pricingService.getRecordIndex(record);
            if (actualIndex == -1) {
                actualIndex = i; // Fallback to sorted position if not found
            }
            
            Map<String, Object> formatted = new java.util.LinkedHashMap<>();
            formatted.put("index", actualIndex);

            formatted.put("instrumentGuid",
                    record.getInstrumentGuid() != null && !record.getInstrumentGuid().trim().isEmpty()
                            ? record.getInstrumentGuid()
                            : "");
            formatted.put("tradeDate", record.getTradeDate() != null ? record.getTradeDate().toString() : "");
            formatted.put("price", formatPriceValue(record));
            formatted.put("exchange",
                    record.getExchange() != null && !record.getExchange().trim().isEmpty() ? record.getExchange() : "");
            formatted.put("productType",
                    record.getProductType() != null && !record.getProductType().trim().isEmpty()
                            ? record.getProductType()
                            : "");
            formatted.put("status", record.isValid() ? "VALID" : "INVALID");
            if (!record.isValid() && record.getValidationError() != null) {
                formatted.put("validationError", record.getValidationError());
            }

            formattedRecords.add(formatted);
        }

        return ResponseEntity.ok(formattedRecords);
    }

    /**
     * Get a specific record by instrument GUID
     * GET /api/pricing/records/{instrumentGuid}
     * GET /api/pricing/records/{instrumentGuid}?index={index}
     */
    @GetMapping("/records/{instrumentGuid}")
    public ResponseEntity<?> getRecord(@PathVariable String instrumentGuid,
            @RequestParam(required = false) Integer index) {
        boolean isEmptyPlaceholder = instrumentGuid != null && instrumentGuid.equalsIgnoreCase("EMPTY");
        String normalizedGuid = isEmptyPlaceholder ? "" : instrumentGuid;

        // If index is provided, use it directly (for null GUIDs or duplicates)
        if (index != null) {
            Optional<PricingRecord> record = pricingService.getRecordByIndex(index);
            if (!record.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Record not found at index: " + index));
            }
            // Build response for the record at this index
            PricingRecord r = record.get();
            Map<String, Object> response = new java.util.LinkedHashMap<>();
            response.put("index", index);
            response.put("instrumentGuid",
                    r.getInstrumentGuid() != null && !r.getInstrumentGuid().trim().isEmpty() ? r.getInstrumentGuid()
                            : "");
            response.put("tradeDate", r.getTradeDate() != null ? r.getTradeDate().toString() : "");
            response.put("price", formatPriceValue(r));
            response.put("exchange",
                    r.getExchange() != null && !r.getExchange().trim().isEmpty() ? r.getExchange() : "");
            response.put("productType",
                    r.getProductType() != null && !r.getProductType().trim().isEmpty() ? r.getProductType() : "");
            response.put("valid", r.isValid());
            if (r.getValidationError() != null) {
                response.put("validationError", r.getValidationError());
            }
            return ResponseEntity.ok(response);
        }

        // If no index provided, search by GUID
        List<PricingRecord> recordsWithGuid = pricingService.getAllRecordsByGuid(normalizedGuid);

        if (recordsWithGuid.isEmpty()) {
            String message = isEmptyPlaceholder
                    ? "Record not found for empty/null GUID."
                    : "Record not found for GUID: " + instrumentGuid;
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", message));
        }

        // If multiple records exist and no index specified, return list for user to
        // choose
        if (recordsWithGuid.size() > 1 && index == null) {
            List<Map<String, Object>> recordsInfo = new java.util.ArrayList<>();
            List<PricingRecord> allRecords = pricingService.getAllRecords();
            String guidDisplay = isEmptyPlaceholder ? "(empty)" : instrumentGuid;

            for (int i = 0; i < allRecords.size(); i++) {
                PricingRecord record = allRecords.get(i);
                String recordGuid = record.getInstrumentGuid();
                boolean matches = isEmptyPlaceholder
                        ? (recordGuid == null || recordGuid.trim().isEmpty())
                        : (recordGuid != null && recordGuid.equals(instrumentGuid));
                if (matches) {
                    Map<String, Object> recordInfo = new java.util.LinkedHashMap<>();
                    recordInfo.put("index", i);
                    recordInfo.put("instrumentGuid",
                            recordGuid != null && !recordGuid.trim().isEmpty() ? recordGuid : "");
                    recordInfo.put("tradeDate", record.getTradeDate() != null ? record.getTradeDate().toString() : "");
                    recordInfo.put("price", formatPriceValue(record));
                    recordInfo.put("exchange",
                            record.getExchange() != null && !record.getExchange().trim().isEmpty()
                                    ? record.getExchange()
                                    : "");
                    recordInfo.put("productType",
                            record.getProductType() != null && !record.getProductType().trim().isEmpty()
                                    ? record.getProductType()
                                    : "");
                    recordInfo.put("status", record.isValid() ? "VALID" : "INVALID");
                    if (record.getValidationError() != null) {
                        recordInfo.put("error", record.getValidationError());
                    }
                    recordsInfo.add(recordInfo);
                }
            }

            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "message", "Multiple records found with GUID: " + guidDisplay,
                            "count", recordsWithGuid.size(),
                            "records", recordsInfo,
                            "instruction", "Use GET /api/pricing/records/" + instrumentGuid
                                    + "?index={index} to view a specific record"));
        }

        // Get by index if specified, otherwise get first match
        Optional<PricingRecord> record;
        if (index != null) {
            record = pricingService.getRecordByIndex(index);
            if (!record.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Record not found at index: " + index));
            }
        } else if (isEmptyPlaceholder) {
            // Safe because duplicates already handled above
            record = Optional.of(recordsWithGuid.get(0));
        } else {
            record = pricingService.getRecordByGuid(normalizedGuid);
        }
        
        if (record.isPresent()) {
            PricingRecord r = record.get();
            Map<String, Object> response = new java.util.LinkedHashMap<>();
            response.put("instrumentGuid",
                    r.getInstrumentGuid() != null && !r.getInstrumentGuid().trim().isEmpty() ? r.getInstrumentGuid()
                            : "");
            response.put("tradeDate", r.getTradeDate() != null ? r.getTradeDate().toString() : "");
            response.put("price", formatPriceValue(r));
            response.put("exchange",
                    r.getExchange() != null && !r.getExchange().trim().isEmpty() ? r.getExchange() : "");
            response.put("productType",
                    r.getProductType() != null && !r.getProductType().trim().isEmpty() ? r.getProductType() : "");
            response.put("status", r.isValid() ? "VALID" : "INVALID");
            if (r.getValidationError() != null) {
                response.put("error", r.getValidationError());
            }
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Record not found for GUID: " + instrumentGuid));
        }
    }

    /**
     * Update a pricing record
     * PUT /api/pricing/records/{instrumentGuid}
     * PUT /api/pricing/records/{instrumentGuid}?index={index}
     */
    @PutMapping("/records/{instrumentGuid}")
    public ResponseEntity<?> updateRecord(@PathVariable String instrumentGuid, 
            @RequestParam(required = false) Integer index,
                                         @RequestBody PricingRecord updatedRecord) {
        // Validate price before updating - must be > 0
        if (updatedRecord.getPrice() != null && updatedRecord.getPrice() <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Price must be greater than zero"));
        }

        boolean isEmptyPlaceholder = instrumentGuid != null && instrumentGuid.equalsIgnoreCase("EMPTY");
        String normalizedGuid = isEmptyPlaceholder ? "" : instrumentGuid;

        // If index is provided, verify GUID matches before updating
        if (index != null) {
            Optional<PricingRecord> recordOpt = pricingService.getRecordByIndex(index);
            if (!recordOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Record not found at index: " + index));
            }

            PricingRecord record = recordOpt.get();
            String recordGuid = record.getInstrumentGuid();

            // For null/empty GUIDs, allow "EMPTY" as placeholder
            boolean isNullGuid = (recordGuid == null || recordGuid.trim().isEmpty());
            boolean isPlaceholder = isEmptyPlaceholder;

            // Verify GUID matches (unless it's a placeholder for null GUID)
            if (!isNullGuid && !isPlaceholder && recordGuid != null && !recordGuid.equals(instrumentGuid)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error",
                                "GUID mismatch: Record at index " + index + " has GUID '" + recordGuid +
                                        "', but you specified '" + instrumentGuid + "'. " +
                                        "This prevents accidental update of wrong records."));
            }

            // If null GUID but not using placeholder, require explicit confirmation
            if (isNullGuid && !isPlaceholder) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error",
                                "Record at index " + index + " has null/empty GUID. " +
                                        "Use 'EMPTY' as GUID placeholder: PUT /api/pricing/records/EMPTY?index="
                                        + index));
            }

            // GUID matches (or placeholder used), proceed with update
            boolean updated = pricingService.updateRecordByIndex(index, updatedRecord);
            if (!updated) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Record not found at index: " + index));
            }

            Optional<PricingRecord> updatedRecordOpt = pricingService.getRecordByIndex(index);
            Map<String, Object> response = new java.util.LinkedHashMap<>();
            response.put("message", "Record updated successfully");
            response.put("index", index);
            response.put("indexNote", "Record remains at index " + index + " after update");
            if (updatedRecordOpt.isPresent()) {
                PricingRecord r = updatedRecordOpt.get();
                Map<String, Object> recordData = new java.util.LinkedHashMap<>();
                recordData.put("instrumentGuid",
                        r.getInstrumentGuid() != null && !r.getInstrumentGuid().trim().isEmpty() ? r.getInstrumentGuid()
                                : "");
                recordData.put("tradeDate", r.getTradeDate() != null ? r.getTradeDate().toString() : "");
                recordData.put("price", r.getPrice() != null ? r.getPrice() : "");
                recordData.put("exchange",
                        r.getExchange() != null && !r.getExchange().trim().isEmpty() ? r.getExchange() : "");
                recordData.put("productType",
                        r.getProductType() != null && !r.getProductType().trim().isEmpty() ? r.getProductType() : "");
                recordData.put("status", r.isValid() ? "VALID" : "INVALID");
                response.put("record", recordData);
            }
            return ResponseEntity.ok(response);
        }

        if (isEmptyPlaceholder) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error",
                    "Instrument GUID is empty/null. Specify the target record using the ?index parameter (use GET /api/pricing/records for indices)."));
        }

        // Check if there are multiple records with same GUID
        List<PricingRecord> recordsWithGuid = pricingService.getAllRecordsByGuid(normalizedGuid);
        String guidDisplay = isEmptyPlaceholder ? "(empty)" : instrumentGuid;

        if (recordsWithGuid.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Record not found for instrument GUID: " + guidDisplay));
        }

        // If multiple records exist (duplicates), index parameter is REQUIRED
        if (recordsWithGuid.size() > 1 && index == null) {
            List<Map<String, Object>> recordsInfo = new java.util.ArrayList<>();
            List<PricingRecord> allRecords = pricingService.getAllRecords();

            for (int i = 0; i < allRecords.size(); i++) {
                PricingRecord record = allRecords.get(i);
                String recordGuid = record.getInstrumentGuid();
                boolean matches = recordGuid != null && recordGuid.equals(instrumentGuid);
                if (matches) {
                    Map<String, Object> recordInfo = new java.util.LinkedHashMap<>();
                    recordInfo.put("index", i);
                    recordInfo.put("instrumentGuid",
                            recordGuid != null && !recordGuid.trim().isEmpty() ? recordGuid : "");
                    recordInfo.put("tradeDate", record.getTradeDate() != null ? record.getTradeDate().toString() : "");
                    recordInfo.put("price", formatPriceValue(record));
                    recordInfo.put("exchange", record.getExchange() != null && !record.getExchange().trim().isEmpty() ? record.getExchange() : "");
                    recordInfo.put("productType", record.getProductType() != null && !record.getProductType().trim().isEmpty() ? record.getProductType() : "");
                    recordInfo.put("status", record.isValid() ? "VALID" : "INVALID");
                    if (record.getValidationError() != null) {
                        recordInfo.put("validationError", record.getValidationError());
                    }
                    recordsInfo.add(recordInfo);
                }
            }

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "error",
                            "Multiple records found with GUID (primary key): " + guidDisplay
                                    + ". Index parameter is required.",
                            "message",
                            "Duplicate GUID detected. Since GUID is the primary key, you must specify which record to update using the ?index parameter.",
                            "count", recordsWithGuid.size(),
                            "records", recordsInfo,
                            "instruction", "Use PUT /api/pricing/records/" + instrumentGuid
                                    + "?index={index} to update a specific record. Example: PUT /api/pricing/records/"
                                    + instrumentGuid + "?index=0"));
        }

        // Update first record with this GUID (primary key - should be unique)
        // Get the index before update
        Optional<PricingRecord> recordBeforeUpdate = pricingService.getRecordByGuid(normalizedGuid);
        int indexBeforeUpdate = -1;
        if (recordBeforeUpdate.isPresent()) {
            indexBeforeUpdate = pricingService.getRecordIndex(recordBeforeUpdate.get());
        }
        
        boolean updated = pricingService.updateRecord(normalizedGuid, updatedRecord);
        
        if (updated) {
            Optional<PricingRecord> updatedRecordOpt = pricingService.getRecordByGuid(normalizedGuid);
            // Get index after update (should be same since we're updating in place)
            int indexAfterUpdate = -1;
            if (updatedRecordOpt.isPresent()) {
                indexAfterUpdate = pricingService.getRecordIndex(updatedRecordOpt.get());
            }

            Map<String, Object> response = new java.util.LinkedHashMap<>();
            response.put("message", "Record updated successfully");
            response.put("index", indexAfterUpdate);
            if (indexBeforeUpdate == indexAfterUpdate) {
                response.put("indexNote", "Record remains at index " + indexAfterUpdate + " after update");
            } else {
                response.put("indexNote", "Index changed from " + indexBeforeUpdate + " to " + indexAfterUpdate);
            }
            if (updatedRecordOpt.isPresent()) {
                PricingRecord r = updatedRecordOpt.get();
                Map<String, Object> record = new java.util.LinkedHashMap<>();
                record.put("instrumentGuid",
                        r.getInstrumentGuid() != null && !r.getInstrumentGuid().trim().isEmpty() ? r.getInstrumentGuid()
                                : "");
                record.put("tradeDate", r.getTradeDate() != null ? r.getTradeDate().toString() : "");
                record.put("price", formatPriceValue(r));
                record.put("exchange",
                        r.getExchange() != null && !r.getExchange().trim().isEmpty() ? r.getExchange() : "");
                record.put("productType",
                        r.getProductType() != null && !r.getProductType().trim().isEmpty() ? r.getProductType() : "");
                record.put("status", r.isValid() ? "VALID" : "INVALID");
                response.put("record", record);
            }
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Record not found for GUID (primary key): " + instrumentGuid));
        }
    }

    /**
     * Delete a pricing record
     * DELETE /api/pricing/records/{instrumentGuid}
     * DELETE /api/pricing/records/{instrumentGuid}?index={index}
     */
    @DeleteMapping("/records/{instrumentGuid}")
    public ResponseEntity<?> deleteRecord(@PathVariable String instrumentGuid,
            @RequestParam(required = false) Integer index) {
        boolean isEmptyPlaceholder = instrumentGuid != null && instrumentGuid.equalsIgnoreCase("EMPTY");
        String normalizedGuid = isEmptyPlaceholder ? "" : instrumentGuid;

        // If index is provided, verify GUID matches before deleting
        if (index != null) {
            Optional<PricingRecord> recordOpt = pricingService.getRecordByIndex(index);
            if (!recordOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Record not found at index: " + index));
            }

            PricingRecord record = recordOpt.get();
            String recordGuid = record.getInstrumentGuid();

            // For null/empty GUIDs, allow "EMPTY" as placeholder
            boolean isNullGuid = (recordGuid == null || recordGuid.trim().isEmpty());
            boolean isPlaceholder = isEmptyPlaceholder;

            // Verify GUID matches (unless it's a placeholder for null GUID)
            if (!isNullGuid && !isPlaceholder && recordGuid != null && !recordGuid.equals(instrumentGuid)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error",
                                "GUID mismatch: Record at index " + index + " has GUID '" + recordGuid +
                                        "', but you specified '" + instrumentGuid + "'. " +
                                        "This prevents accidental deletion of wrong records."));
            }

            // If null GUID but not using placeholder, require explicit confirmation
            if (isNullGuid && !isPlaceholder) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error",
                                "Record at index " + index + " has null/empty GUID. " +
                                        "Use 'EMPTY' as GUID placeholder: DELETE /api/pricing/records/EMPTY?index="
                                        + index));
            }

            // GUID matches (or placeholder used), proceed with deletion
            // Get record details before deletion
            PricingRecord recordToDelete = recordOpt.get();
            String deletedGuid = recordToDelete.getInstrumentGuid() != null ? recordToDelete.getInstrumentGuid()
                    : "EMPTY";
            
            boolean deleted = pricingService.deleteRecordByIndex(index);
            if (!deleted) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Record not found at index: " + index));
            }
            
            Map<String, Object> response = new java.util.LinkedHashMap<>();
            response.put("message", "Record deleted successfully!");
            response.put("deletedIndex", index);
            response.put("deletedGuid", deletedGuid);
            return ResponseEntity.ok(response);
        }

        if (isEmptyPlaceholder) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error",
                            "Instrument GUID is empty/null. Specify the record index using the ?index parameter (use GET /api/pricing/records for indices)."));
        }

        // Check if there are multiple records with same GUID
        List<PricingRecord> recordsWithGuid = pricingService.getAllRecordsByGuid(normalizedGuid);
        String guidDisplay = isEmptyPlaceholder ? "(empty)" : instrumentGuid;

        if (recordsWithGuid.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Record not found for instrument GUID: " + guidDisplay));
        }

        // If multiple records exist (duplicates), index parameter is REQUIRED
        if (recordsWithGuid.size() > 1 && index == null) {
            List<Map<String, Object>> recordsInfo = new java.util.ArrayList<>();
            List<PricingRecord> allRecords = pricingService.getAllRecords();

            for (int i = 0; i < allRecords.size(); i++) {
                PricingRecord record = allRecords.get(i);
                if (record.getInstrumentGuid() != null && record.getInstrumentGuid().equals(instrumentGuid)) {
                    Map<String, Object> recordInfo = new java.util.LinkedHashMap<>();
                    recordInfo.put("index", i);
                    recordInfo.put("instrumentGuid", record.getInstrumentGuid());
                    recordInfo.put("tradeDate", record.getTradeDate() != null ? record.getTradeDate().toString() : "");
                    recordInfo.put("price", formatPriceValue(record));
                    recordInfo.put("exchange", record.getExchange() != null && !record.getExchange().trim().isEmpty() ? record.getExchange() : "");
                    recordInfo.put("productType", record.getProductType() != null && !record.getProductType().trim().isEmpty() ? record.getProductType() : "");
                    recordInfo.put("valid", record.isValid());
                    recordInfo.put("validationError", record.getValidationError() != null ? record.getValidationError() : "");
                    recordsInfo.add(recordInfo);
                }
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "error",
                            "Multiple records found with GUID (primary key): " + guidDisplay
                                    + ". Index parameter is required.",
                            "message",
                            "Duplicate GUID detected. Since GUID is the primary key, you must specify which record to delete using the ?index parameter.",
                            "count", recordsWithGuid.size(),
                            "records", recordsInfo,
                            "instruction", "Use DELETE /api/pricing/records/" + instrumentGuid
                                    + "?index={index} to delete a specific record"));
        }

        // Delete first record with this GUID (primary key - should be unique)
        // Get the index before deletion
        Optional<PricingRecord> recordBeforeDelete = pricingService.getRecordByGuid(normalizedGuid);
        int indexBeforeDelete = -1;
        if (recordBeforeDelete.isPresent()) {
            indexBeforeDelete = pricingService.getRecordIndex(recordBeforeDelete.get());
        }
        
        boolean deleted = pricingService.deleteRecord(normalizedGuid);
        
        if (deleted) {
            Map<String, Object> response = new java.util.LinkedHashMap<>();
            response.put("message", "Record deleted successfully!");
            response.put("deletedIndex", indexBeforeDelete);
            response.put("deletedGuid", guidDisplay);
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Record not found for GUID (primary key): " + guidDisplay));
        }
    }

    /**
     * Correct an invalid record
     * POST /api/pricing/records/{instrumentGuid}/correct
     * POST /api/pricing/records/{instrumentGuid}/correct?index={index}
     */
    @PostMapping("/records/{instrumentGuid}/correct")
    public ResponseEntity<?> correctRecord(@PathVariable String instrumentGuid,
            @RequestParam(required = false) Integer index,
                                          @RequestBody PricingRecord correction) {
        // Validate price before correcting - must be > 0
        if (correction.getPrice() != null && correction.getPrice() <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Price must be greater than zero"));
        }

        boolean isEmptyPlaceholder = instrumentGuid != null && instrumentGuid.equalsIgnoreCase("EMPTY");
        String normalizedGuid = isEmptyPlaceholder ? "" : instrumentGuid;

        // If index is provided, verify GUID matches before correcting
        if (index != null) {
            Optional<PricingRecord> recordOpt = pricingService.getRecordByIndex(index);
            if (!recordOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Record not found at index: " + index));
            }

            PricingRecord record = recordOpt.get();
            String recordGuid = record.getInstrumentGuid();

            // For null/empty GUIDs, allow "EMPTY" as placeholder
            boolean isNullGuid = (recordGuid == null || recordGuid.trim().isEmpty());
            boolean isPlaceholder = isEmptyPlaceholder;

            // Verify GUID matches (unless it's a placeholder for null GUID)
            if (!isNullGuid && !isPlaceholder && recordGuid != null && !recordGuid.equals(instrumentGuid)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error",
                                "GUID mismatch: Record at index " + index + " has GUID '" + recordGuid +
                                        "', but you specified '" + instrumentGuid + "'. " +
                                        "This prevents accidental correction of wrong records."));
            }

            // If null GUID but not using placeholder, require explicit confirmation
            if (isNullGuid && !isPlaceholder) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error",
                                "Record at index " + index + " has null/empty GUID. " +
                                        "Use 'EMPTY' as GUID placeholder: POST /api/pricing/records/EMPTY/correct?index="
                                        + index));
            }

            // GUID matches (or placeholder used), proceed with correction
            boolean corrected = pricingService.correctRecordByIndex(index, correction);
            if (!corrected) {
                // Correction failed for another reason (e.g., duplicate GUID)
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error",
                                "Correction failed. The new GUID may already exist or price is invalid."));
            }

            Optional<PricingRecord> updatedRecord = pricingService.getRecordByIndex(index);
            Map<String, Object> response = new java.util.LinkedHashMap<>();
            response.put("message", "Record corrected successfully!");
            if (updatedRecord.isPresent() && updatedRecord.get().isValid()) {
                response.put("recordIsNowValid", true);
            }
            if (updatedRecord.isPresent()) {
                PricingRecord r = updatedRecord.get();
                Map<String, Object> recordData = new java.util.LinkedHashMap<>();
                recordData.put("instrumentGuid",
                        r.getInstrumentGuid() != null && !r.getInstrumentGuid().trim().isEmpty() ? r.getInstrumentGuid()
                                : "");
                recordData.put("tradeDate", r.getTradeDate() != null ? r.getTradeDate().toString() : "");
                recordData.put("price", r.getPrice() != null ? r.getPrice() : "");
                recordData.put("exchange",
                        r.getExchange() != null && !r.getExchange().trim().isEmpty() ? r.getExchange() : "");
                recordData.put("productType",
                        r.getProductType() != null && !r.getProductType().trim().isEmpty() ? r.getProductType() : "");
                recordData.put("status", r.isValid() ? "VALID" : "INVALID");
                if (r.getValidationError() != null) {
                    recordData.put("error", r.getValidationError());
                }
                response.put("record", recordData);
            }
            return ResponseEntity.ok(response);
        }

        if (isEmptyPlaceholder) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error",
                            "Instrument GUID is empty/null. Specify the record index using the ?index parameter (use GET /api/pricing/records for indices)."));
        }

        // Check if there are multiple records with same GUID
        List<PricingRecord> recordsWithGuid = pricingService.getAllRecordsByGuid(normalizedGuid);
        String guidDisplay = isEmptyPlaceholder ? "(empty)" : instrumentGuid;

        if (recordsWithGuid.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Record not found for GUID: " + guidDisplay));
        }

        // If multiple records exist (duplicates), index parameter is REQUIRED
        if (recordsWithGuid.size() > 1 && index == null) {
            List<Map<String, Object>> recordsInfo = new java.util.ArrayList<>();
            List<PricingRecord> allRecords = pricingService.getAllRecords();
            List<Integer> matchingIndices = new java.util.ArrayList<>();

            for (int i = 0; i < allRecords.size(); i++) {
                PricingRecord record = allRecords.get(i);
                if (record.getInstrumentGuid() != null && record.getInstrumentGuid().equals(instrumentGuid)) {
                    matchingIndices.add(i);
                    Map<String, Object> recordInfo = new java.util.LinkedHashMap<>();
                    recordInfo.put("index", i);
                    recordInfo.put("instrumentGuid", record.getInstrumentGuid());
                    recordInfo.put("tradeDate", record.getTradeDate() != null ? record.getTradeDate().toString() : "");
                    recordInfo.put("price", formatPriceValue(record));
                    recordInfo.put("exchange", record.getExchange() != null && !record.getExchange().trim().isEmpty() ? record.getExchange() : "");
                    recordInfo.put("productType", record.getProductType() != null && !record.getProductType().trim().isEmpty() ? record.getProductType() : "");
                    recordInfo.put("status", record.isValid() ? "VALID" : "INVALID");
                    if (record.getValidationError() != null) {
                        recordInfo.put("validationError", record.getValidationError());
                    }
                    recordsInfo.add(recordInfo);
                }
            }

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "error",
                            "Multiple records found with GUID (primary key): " + guidDisplay
                                    + ". Index parameter is required.",
                            "message",
                            "Duplicate GUID detected. Since GUID is the primary key, you must specify which record to correct using the ?index parameter.",
                            "count", recordsWithGuid.size(),
                            "records", recordsInfo,
                            "instruction", "Use POST /api/pricing/records/" + instrumentGuid
                                    + "/correct?index={index} to correct a specific record. Example: POST /api/pricing/records/"
                                    + instrumentGuid + "/correct?index=0"));
        }

        // Correct by index if specified, otherwise correct first match
        boolean corrected;
        if (index != null) {
            corrected = pricingService.correctRecordByIndex(index, correction);
            if (!corrected) {
                // Check if record exists at this index
                Optional<PricingRecord> recordAtIndex = pricingService.getRecordByIndex(index);
                if (!recordAtIndex.isPresent()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "Record not found at index: " + index));
                } else {
                    // Correction failed for another reason (e.g., duplicate GUID)
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error",
                                    "Correction failed. The new GUID may already exist or price is invalid."));
                }
            }
        } else {
            corrected = pricingService.correctRecord(normalizedGuid, correction);
        if (!corrected) {
            // Check if record exists
                Optional<PricingRecord> record = pricingService.getRecordByGuid(normalizedGuid);
            if (!record.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "Record not found for GUID (primary key): " + guidDisplay));
                } else {
                    // Correction failed for another reason (e.g., duplicate GUID)
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error",
                                    "Correction failed. The new GUID may already exist or price is invalid."));
                }
            }
        }
        
        if (corrected) {
            Optional<PricingRecord> updatedRecord;
            int finalIndex = -1;
            if (index != null) {
                updatedRecord = pricingService.getRecordByIndex(index);
                finalIndex = index;
            } else {
                updatedRecord = pricingService.getRecordByGuid(normalizedGuid);
                if (updatedRecord.isPresent()) {
                    finalIndex = pricingService.getRecordIndex(updatedRecord.get());
                }
            }

            Map<String, Object> response = new java.util.LinkedHashMap<>();
            response.put("message", "Record corrected successfully!");
            response.put("index", finalIndex);
            if (index != null) {
                response.put("indexNote", "Record remains at index " + finalIndex + " after correction");
            } else {
                response.put("indexNote", "Record is at index " + finalIndex + " after correction");
            }
            if (updatedRecord.isPresent() && updatedRecord.get().isValid()) {
                response.put("recordIsNowValid", true);
            }
            if (updatedRecord.isPresent()) {
                PricingRecord r = updatedRecord.get();
                Map<String, Object> record = new java.util.LinkedHashMap<>();
                record.put("instrumentGuid",
                        r.getInstrumentGuid() != null && !r.getInstrumentGuid().trim().isEmpty() ? r.getInstrumentGuid()
                                : "");
                record.put("tradeDate", r.getTradeDate() != null ? r.getTradeDate().toString() : "");
                record.put("price", formatPriceValue(r));
                record.put("exchange",
                        r.getExchange() != null && !r.getExchange().trim().isEmpty() ? r.getExchange() : "");
                record.put("productType",
                        r.getProductType() != null && !r.getProductType().trim().isEmpty() ? r.getProductType() : "");
                record.put("status", r.isValid() ? "VALID" : "INVALID");
                if (r.getValidationError() != null) {
                    record.put("error", r.getValidationError());
                }
                response.put("record", record);
            }
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Record not found for GUID: " + instrumentGuid));
        }
    }
}
