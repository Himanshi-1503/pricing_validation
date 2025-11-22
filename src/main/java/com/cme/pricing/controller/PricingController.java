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
            // Format price: show invalid values, blank for missing
            Object price;
            if (record.getPrice() != null) {
                price = record.getPrice();
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
            recordInfo.put("price", price);
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
                // Format price: show invalid values, blank for missing
                Object price;
                if (record.getPrice() != null) {
                    price = record.getPrice();
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
                dupInfo.put("price", price != null ? price : "");
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

            // Format price: show invalid values, blank for missing
            Object price;
            if (record.getPrice() != null) {
                price = record.getPrice();
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

            recordRow.put("guid",
                    record.getInstrumentGuid() != null && !record.getInstrumentGuid().trim().isEmpty()
                            ? record.getInstrumentGuid()
                            : "");
            recordRow.put("date", record.getTradeDate() != null ? record.getTradeDate().toString() : "");
            recordRow.put("price", price != null ? price : "");
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

        // Format records matching CLI format
        List<Map<String, Object>> formattedRecords = new java.util.ArrayList<>();
        for (PricingRecord record : records) {
            Map<String, Object> formatted = new java.util.LinkedHashMap<>();

            // Format price: show invalid values, null for missing
            Object price;
            if (record.getPrice() != null) {
                price = record.getPrice();
            } else {
                // Check if it's an invalid value
                String error = record.getValidationError();
                if (error != null && error.contains("Invalid price format: ")) {
                    int startIndex = error.indexOf("Invalid price format: ") + "Invalid price format: ".length();
                    int endIndex = error.indexOf(";", startIndex);
                    price = (endIndex == -1) ? error.substring(startIndex).trim()
                            : error.substring(startIndex, endIndex).trim();
                } else {
                    price = null;
                }
            }

            formatted.put("instrumentGuid",
                    record.getInstrumentGuid() != null && !record.getInstrumentGuid().trim().isEmpty()
                            ? record.getInstrumentGuid()
                            : "");
            formatted.put("tradeDate", record.getTradeDate() != null ? record.getTradeDate().toString() : "");
            formatted.put("price", price != null ? price : "");
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
        List<PricingRecord> recordsWithGuid = pricingService.getAllRecordsByGuid(instrumentGuid);

        if (recordsWithGuid.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Record not found for GUID: " + instrumentGuid));
        }

        // If multiple records exist and no index specified, return list for user to
        // choose
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
                    // Format price: show invalid values, blank for missing
                    Object price;
                    if (record.getPrice() != null) {
                        price = record.getPrice();
                    } else {
                        String error = record.getValidationError();
                        if (error != null && error.contains("Invalid price format: ")) {
                            int startIndex = error.indexOf("Invalid price format: ")
                                    + "Invalid price format: ".length();
                            int endIndex = error.indexOf(";", startIndex);
                            price = (endIndex == -1) ? error.substring(startIndex).trim()
                                    : error.substring(startIndex, endIndex).trim();
                        } else {
                            price = record.getOriginalPriceValue() != null ? record.getOriginalPriceValue() : "";
                        }
                    }
                    recordInfo.put("price", price != null ? price : "");
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
                            "message", "Multiple records found with GUID: " + instrumentGuid,
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
        } else {
            record = pricingService.getRecordByGuid(instrumentGuid);
        }
        
        if (record.isPresent()) {
            PricingRecord r = record.get();
            Map<String, Object> response = new java.util.LinkedHashMap<>();
            response.put("instrumentGuid",
                    r.getInstrumentGuid() != null && !r.getInstrumentGuid().trim().isEmpty() ? r.getInstrumentGuid()
                            : "");
            response.put("tradeDate", r.getTradeDate() != null ? r.getTradeDate().toString() : "");
            // Format price: show invalid values, blank for missing
            Object price;
            if (r.getPrice() != null) {
                price = r.getPrice();
            } else {
                String error = r.getValidationError();
                if (error != null && error.contains("Invalid price format: ")) {
                    int startIndex = error.indexOf("Invalid price format: ") + "Invalid price format: ".length();
                    int endIndex = error.indexOf(";", startIndex);
                    price = (endIndex == -1) ? error.substring(startIndex).trim()
                            : error.substring(startIndex, endIndex).trim();
                } else {
                    price = "";
                }
            }
            response.put("price", price != null ? price : "");
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

        // If index is provided, update by index directly (for null GUID records)
        if (index != null) {
            boolean updated = pricingService.updateRecordByIndex(index, updatedRecord);
            if (!updated) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Record not found at index: " + index));
            }
            
            Optional<PricingRecord> updatedRecordOpt = pricingService.getRecordByIndex(index);
            Map<String, Object> response = new java.util.LinkedHashMap<>();
            response.put("message", "Record updated successfully");
            if (updatedRecordOpt.isPresent()) {
                PricingRecord r = updatedRecordOpt.get();
                Map<String, Object> record = new java.util.LinkedHashMap<>();
                record.put("instrumentGuid",
                        r.getInstrumentGuid() != null && !r.getInstrumentGuid().trim().isEmpty() ? r.getInstrumentGuid()
                                : "");
                record.put("tradeDate", r.getTradeDate() != null ? r.getTradeDate().toString() : "");
                record.put("price", r.getPrice() != null ? r.getPrice() : "");
                record.put("exchange",
                        r.getExchange() != null && !r.getExchange().trim().isEmpty() ? r.getExchange() : "");
                record.put("productType",
                        r.getProductType() != null && !r.getProductType().trim().isEmpty() ? r.getProductType() : "");
                record.put("status", r.isValid() ? "VALID" : "INVALID");
                response.put("record", record);
            }
            return ResponseEntity.ok(response);
        }

        // Check if there are multiple records with same GUID
        List<PricingRecord> recordsWithGuid = pricingService.getAllRecordsByGuid(instrumentGuid);

        if (recordsWithGuid.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Record not found for instrument GUID: " + instrumentGuid));
        }

        // If multiple records exist and no index specified, return list for user to
        // choose
        if (recordsWithGuid.size() > 1 && index == null) {
            List<Map<String, Object>> recordsInfo = new java.util.ArrayList<>();
            List<PricingRecord> allRecords = pricingService.getAllRecords();

            for (int i = 0; i < allRecords.size(); i++) {
                PricingRecord record = allRecords.get(i);
                if (record.getInstrumentGuid() != null && record.getInstrumentGuid().equals(instrumentGuid)) {
                    Map<String, Object> recordInfo = new java.util.LinkedHashMap<>();
                    recordInfo.put("index", i);
                    recordInfo.put("instrumentGuid", record.getInstrumentGuid());
                    recordInfo.put("tradeDate", record.getTradeDate());
                    Object displayPrice = record.getPrice() != null ? record.getPrice()
                            : (record.getOriginalPriceValue() != null ? record.getOriginalPriceValue() : null);
                    recordInfo.put("price", displayPrice);
                    recordInfo.put("exchange", record.getExchange());
                    recordInfo.put("productType", record.getProductType());
                    recordInfo.put("status", record.isValid() ? "VALID" : "INVALID");
                    if (record.getValidationError() != null) {
                        recordInfo.put("validationError", record.getValidationError());
                    }
                    recordsInfo.add(recordInfo);
                }
            }

            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "message", "Multiple records found with GUID: " + instrumentGuid,
                            "count", recordsWithGuid.size(),
                            "records", recordsInfo,
                            "instruction", "Use PUT /api/pricing/records/" + instrumentGuid
                                    + "?index={index} to update a specific record"));
        }

        // Update by index if specified, otherwise update first match
        boolean updated;
        if (index != null) {
            updated = pricingService.updateRecordByIndex(index, updatedRecord);
            if (!updated) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Record not found at index: " + index));
            }
        } else {
            updated = pricingService.updateRecord(instrumentGuid, updatedRecord);
        }

        if (updated) {
            Optional<PricingRecord> updatedRecordOpt;
            if (index != null) {
                updatedRecordOpt = pricingService.getRecordByIndex(index);
            } else {
                updatedRecordOpt = pricingService.getRecordByGuid(instrumentGuid);
            }

            Map<String, Object> response = new java.util.LinkedHashMap<>();
            response.put("message", "Record updated successfully");
            if (updatedRecordOpt.isPresent()) {
                PricingRecord r = updatedRecordOpt.get();
                Map<String, Object> record = new java.util.LinkedHashMap<>();
                record.put("instrumentGuid",
                        r.getInstrumentGuid() != null && !r.getInstrumentGuid().trim().isEmpty() ? r.getInstrumentGuid()
                                : "");
                record.put("tradeDate", r.getTradeDate() != null ? r.getTradeDate().toString() : "");
                record.put("price", r.getPrice() != null ? r.getPrice() : "");
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
                    .body(Map.of("error", "Record not found for GUID: " + instrumentGuid));
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
        // If index is provided, delete by index directly (for null GUID records)
        if (index != null) {
            boolean deleted = pricingService.deleteRecordByIndex(index);
            if (!deleted) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Record not found at index: " + index));
            }
            return ResponseEntity.ok(Map.of("message", "Record deleted successfully!"));
        }

        // Check if there are multiple records with same GUID
        List<PricingRecord> recordsWithGuid = pricingService.getAllRecordsByGuid(instrumentGuid);

        if (recordsWithGuid.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Record not found for instrument GUID: " + instrumentGuid));
        }

        // If multiple records exist and no index specified, return list for user to
        // choose
        if (recordsWithGuid.size() > 1 && index == null) {
            List<Map<String, Object>> recordsInfo = new java.util.ArrayList<>();
            List<PricingRecord> allRecords = pricingService.getAllRecords();

            for (int i = 0; i < allRecords.size(); i++) {
                PricingRecord record = allRecords.get(i);
                if (record.getInstrumentGuid() != null && record.getInstrumentGuid().equals(instrumentGuid)) {
                    Map<String, Object> recordInfo = new java.util.LinkedHashMap<>();
                    recordInfo.put("index", i);
                    recordInfo.put("instrumentGuid", record.getInstrumentGuid());
                    recordInfo.put("tradeDate", record.getTradeDate());
                    // Use display price (shows original invalid value if applicable)
                    Object displayPrice = record.getPrice() != null ? record.getPrice()
                            : (record.getOriginalPriceValue() != null ? record.getOriginalPriceValue() : null);
                    recordInfo.put("price", displayPrice);
                    recordInfo.put("exchange", record.getExchange());
                    recordInfo.put("productType", record.getProductType());
                    recordInfo.put("valid", record.isValid());
                    recordInfo.put("validationError", record.getValidationError());
                    recordsInfo.add(recordInfo);
                }
            }
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "message", "Multiple records found with GUID: " + instrumentGuid,
                            "count", recordsWithGuid.size(),
                            "records", recordsInfo,
                            "instruction", "Use DELETE /api/pricing/records/" + instrumentGuid
                                    + "?index={index} to delete a specific record"));
        }

        // Delete first record with this GUID
        boolean deleted = pricingService.deleteRecord(instrumentGuid);
        
        if (deleted) {
            return ResponseEntity.ok(Map.of("message", "Record deleted successfully!"));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Record not found for GUID: " + instrumentGuid));
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

        // If index is provided, correct by index directly (for null GUID records)
        if (index != null) {
            boolean corrected = pricingService.correctRecordByIndex(index, correction);
            if (!corrected) {
                // Check if record exists at this index
                Optional<PricingRecord> recordAtIndex = pricingService.getRecordByIndex(index);
                if (!recordAtIndex.isPresent()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "Record not found at index: " + index));
                } else {
                    // Correction failed for another reason (e.g., duplicate GUID)
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Correction failed. The new GUID may already exist or price is invalid."));
                }
            }
            
            Optional<PricingRecord> updatedRecord = pricingService.getRecordByIndex(index);
            Map<String, Object> response = new java.util.LinkedHashMap<>();
            response.put("message", "Record corrected successfully!");
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
                record.put("price", r.getPrice() != null ? r.getPrice() : "");
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
        }

        // Check if there are multiple records with same GUID
        List<PricingRecord> recordsWithGuid = pricingService.getAllRecordsByGuid(instrumentGuid);

        if (recordsWithGuid.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Record not found for GUID: " + instrumentGuid));
        }

        // If multiple records exist and no index specified, return list for user to
        // choose
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
                    recordInfo.put("tradeDate", record.getTradeDate());
                    Object displayPrice = record.getPrice() != null ? record.getPrice()
                            : (record.getOriginalPriceValue() != null ? record.getOriginalPriceValue() : null);
                    recordInfo.put("price", displayPrice);
                    recordInfo.put("exchange", record.getExchange());
                    recordInfo.put("productType", record.getProductType());
                    recordInfo.put("status", record.isValid() ? "VALID" : "INVALID");
                    if (record.getValidationError() != null) {
                        recordInfo.put("validationError", record.getValidationError());
                    }
                    recordsInfo.add(recordInfo);
                }
            }

            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "message", "Multiple records found with GUID: " + instrumentGuid,
                            "count", recordsWithGuid.size(),
                            "records", recordsInfo,
                            "instruction", "Use POST /api/pricing/records/" + instrumentGuid
                                    + "/correct?index={index} to correct a specific record"));
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
                            .body(Map.of("error", "Correction failed. The new GUID may already exist or price is invalid."));
                }
            }
        } else {
            corrected = pricingService.correctRecord(instrumentGuid, correction);
            if (!corrected) {
                // Check if record exists
                Optional<PricingRecord> record = pricingService.getRecordByGuid(instrumentGuid);
                if (!record.isPresent()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "Record not found for GUID: " + instrumentGuid));
                } else {
                    // Correction failed for another reason (e.g., duplicate GUID)
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Correction failed. The new GUID may already exist or price is invalid."));
                }
            }
        }
        
        if (corrected) {
            Optional<PricingRecord> updatedRecord;
            if (index != null) {
                updatedRecord = pricingService.getRecordByIndex(index);
            } else {
                updatedRecord = pricingService.getRecordByGuid(instrumentGuid);
            }

            Map<String, Object> response = new java.util.LinkedHashMap<>();
            response.put("message", "Record corrected successfully!");
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
                record.put("price", r.getPrice() != null ? r.getPrice() : "");
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
                    .body(Map.of("error", "Record not found for GUID: " + instrumentGuid));
        }
    }
}
