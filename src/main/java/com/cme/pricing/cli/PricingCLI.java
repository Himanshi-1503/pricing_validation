package com.cme.pricing.cli;

import com.cme.pricing.model.PricingRecord;
import com.cme.pricing.model.ValidationReport;
import com.cme.pricing.report.ReportGenerator;
import com.cme.pricing.service.PricingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Command-line interface for Pricing Data Validation Utility
 */
@Component
public class PricingCLI implements CommandLineRunner {

    @Autowired
    private PricingService pricingService;

    @Autowired
    private ReportGenerator reportGenerator;

    private Scanner scanner = new Scanner(System.in);

    @Override
    public void run(String... args) {
        // Only run CLI if --cli argument is provided
        if (args.length > 0 && args[0].equals("--cli")) {
            showWelcome();
            runCLI();
        }
    }

    private void showWelcome() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("  Pricing Data Validation & Reporting Utility");
        System.out.println("=".repeat(60));
        System.out.println();
    }

    private void runCLI() {
        while (true) {
            showMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    loadData();
                    break;
                case "2":
                    viewReport();
                    break;
                case "3":
                    viewRecord();
                    break;
                case "4":
                    updateRecord();
                    break;
                case "5":
                    deleteRecord();
                    break;
                case "6":
                    generateReportFile();
                    break;
                case "7":
                    System.out.println("\nExiting... Goodbye!");
                    return;
                default:
                    System.out.println("\nInvalid choice. Please try again.");
            }
            System.out.println("\nPress Enter to continue...");
            scanner.nextLine();
        }
    }

    private void showMenu() {
        System.out.println("\n" + "-".repeat(60));
        System.out.println("MENU:");
        System.out.println("1. Load and validate CSV file");
        System.out.println("2. View validation report");
        System.out.println("3. View specific record");
        System.out.println("4. Update record");
        System.out.println("5. Delete record");
        System.out.println("6. Generate text report file");
        System.out.println("7. Exit");
        System.out.println("-".repeat(60));
        System.out.print("Enter your choice: ");
    }

    private void loadData() {
        System.out.println("\n--- Load and Validate CSV File ---");
        System.out.print("Enter file path (e.g., sample_data/pricing_data.csv): ");
        String filePath = scanner.nextLine().trim();

        if (filePath.isEmpty()) {
            System.out.println("Error: File path cannot be empty.");
            return;
        }

        try {
            ValidationReport report = pricingService.loadAndValidateData(filePath);
            System.out.println("\n✓ Data loaded and validated successfully!");
            System.out.println("\nSummary:");
            System.out.println("  Total Records: " + report.getTotalRecords());
            System.out.println("  Valid Records: " + report.getValidRecords());
            System.out.println("  Invalid Records: " + report.getInvalidRecords());
            System.out.println("  Duplicate Records: " + report.getDuplicateRecords());

            // Calculate and display total missing values
            int totalMissing = report.getMissingPriceRecords() +
                    report.getMissingInstrumentGuidRecords() +
                    report.getMissingTradeDateRecords() +
                    report.getMissingExchangeRecords() +
                    report.getMissingProductTypeRecords();
            if (totalMissing > 0) {
                System.out.println("  Missing Records: " + totalMissing);
            }
        } catch (IOException e) {
            System.out.println("Error: Failed to load file - " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void viewReport() {
        System.out.println("\n--- Validation Report ---");

        ValidationReport report = pricingService.generateReport();

        if (report.getTotalRecords() == 0) {
            System.out.println("No data loaded. Please load a CSV file first (Option 1).");
            return;
        }

        System.out.println("\nSUMMARY:");
        System.out.println("  Total Records: " + report.getTotalRecords());
        System.out.println("  Valid Records: " + report.getValidRecords());
        System.out.println("  Invalid Records: " + report.getInvalidRecords());
        System.out.println("  Duplicate Records: " + report.getDuplicateRecords());

        // Calculate and display total missing values
        int totalMissing = report.getMissingPriceRecords() +
                report.getMissingInstrumentGuidRecords() +
                report.getMissingTradeDateRecords() +
                report.getMissingExchangeRecords() +
                report.getMissingProductTypeRecords();
        if (totalMissing > 0) {
            System.out.println("  Missing Records: " + totalMissing);
        }

        if (report.getInvalidRecords() > 0) {
            System.out.println("\nERROR BREAKDOWN:");
            if (report.getMissingPriceRecords() > 0) {
                System.out.println("  Missing Price: " + report.getMissingPriceRecords());
            }
            if (report.getInvalidPriceFormatRecords() > 0) {
                System.out.println("  Invalid Price Format: " + report.getInvalidPriceFormatRecords());
            }
            if (report.getMissingInstrumentGuidRecords() > 0) {
                System.out.println("  Missing Instrument GUID: " + report.getMissingInstrumentGuidRecords());
            }
            if (report.getMissingTradeDateRecords() > 0) {
                System.out.println("  Missing Trade Date: " + report.getMissingTradeDateRecords());
            }
            if (report.getMissingExchangeRecords() > 0) {
                System.out.println("  Missing Exchange: " + report.getMissingExchangeRecords());
            }
            if (report.getMissingProductTypeRecords() > 0) {
                System.out.println("  Missing Product Type: " + report.getMissingProductTypeRecords());
            }
            if (report.getInvalidExchangeRecords() > 0) {
                System.out.println("  Invalid Exchange: " + report.getInvalidExchangeRecords());
            }
            if (report.getInvalidProductTypeRecords() > 0) {
                System.out.println("  Invalid Product Type: " + report.getInvalidProductTypeRecords());
            }
        }

        System.out.println("\n--- All Records ---");
        List<PricingRecord> records = pricingService.getAllRecordsSorted();
        System.out.printf("%-15s %-12s %-10s %-8s %-8s %-8s%n",
                "GUID", "Trade Date", "Price", "Exchange", "Product", "Status");
        System.out.println("-".repeat(80));

        for (PricingRecord record : records) {
            String guid = record.getInstrumentGuid() != null && !record.getInstrumentGuid().trim().isEmpty()
                    ? record.getInstrumentGuid()
                    : "(empty)";
            String date = record.getTradeDate() != null ? record.getTradeDate().toString() : "N/A";
            String price = record.getPrice() != null ? String.format("%.2f", record.getPrice()) : "N/A";
            String exchange = record.getExchange() != null ? record.getExchange() : "N/A";
            String productType = record.getProductType() != null ? record.getProductType() : "N/A";
            String status = record.isValid() ? "VALID" : "INVALID";

            System.out.printf("%-15s %-12s %-10s %-8s %-8s %-8s%n",
                    guid, date, price, exchange, productType, status);
        }
    }

    private void viewRecord() {
        System.out.println("\n--- View Specific Record ---");
        System.out.print("Enter Instrument GUID: ");
        String guid = scanner.nextLine().trim();

        if (guid.isEmpty()) {
            System.out.println("Error: GUID cannot be empty.");
            return;
        }

        List<PricingRecord> records = pricingService.getAllRecordsByGuid(guid);

        if (records.isEmpty()) {
            System.out.println("No record found with GUID: " + guid);
            return;
        }

        if (records.size() == 1) {
            displayRecord(records.get(0));
        } else {
            // Multiple records found - show with actual indices from full list
            System.out.println("\n⚠ Multiple records found with GUID: " + guid + " (duplicates detected)");
            System.out.println("Please select by index from the full records list:");

            List<PricingRecord> allRecords = pricingService.getAllRecords();
            List<Integer> matchingIndices = new java.util.ArrayList<>();

            for (int i = 0; i < allRecords.size(); i++) {
                PricingRecord record = allRecords.get(i);
                if (record.getInstrumentGuid() != null && record.getInstrumentGuid().equals(guid)) {
                    matchingIndices.add(i);
                    System.out.println("  [Index " + i + "] " + formatRecordSummary(record));
                }
            }

            System.out.print("Enter index: ");
            try {
                int index = Integer.parseInt(scanner.nextLine().trim());
                if (matchingIndices.contains(index)) {
                    Optional<PricingRecord> record = pricingService.getRecordByIndex(index);
                    if (record.isPresent()) {
                        displayRecord(record.get());
                    } else {
                        System.out.println("Record not found at index: " + index);
                    }
                } else {
                    System.out.println("Invalid index. Please use one of the indices shown above.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid index format.");
            }
        }
    }

    private void displayRecord(PricingRecord record) {
        System.out.println("\n--- Record Details ---");
        System.out.println(
                "Instrument GUID: " + (record.getInstrumentGuid() != null ? record.getInstrumentGuid() : "(empty)"));
        System.out.println("Trade Date: " + (record.getTradeDate() != null ? record.getTradeDate() : "N/A"));
        System.out.println("Price: " + (record.getPrice() != null ? record.getPrice() : "N/A"));
        System.out.println("Exchange: " + (record.getExchange() != null ? record.getExchange() : "N/A"));
        System.out.println("Product Type: " + (record.getProductType() != null ? record.getProductType() : "N/A"));
        System.out.println("Status: " + (record.isValid() ? "VALID" : "INVALID"));
        if (!record.isValid() && record.getValidationError() != null) {
            System.out.println("Error: " + record.getValidationError());
        }
    }

    private String formatRecordSummary(PricingRecord record) {
        String date = record.getTradeDate() != null ? record.getTradeDate().toString() : "N/A";
        String price = record.getPrice() != null ? String.format("%.2f", record.getPrice()) : "N/A";
        return "Date: " + date + ", Price: " + price + ", Status: " + (record.isValid() ? "VALID" : "INVALID");
    }

    private void updateRecord() {
        System.out.println("\n--- Update Record ---");
        System.out.print("Enter Instrument GUID: ");
        String guid = scanner.nextLine().trim();

        if (guid.isEmpty()) {
            System.out.println("Error: GUID cannot be empty.");
            return;
        }

        List<PricingRecord> records = pricingService.getAllRecordsByGuid(guid);

        if (records.isEmpty()) {
            System.out.println("No record found with GUID: " + guid);
            return;
        }

        Integer targetIndex = null;
        if (records.size() == 1) {
            // Find the actual index in the full list
            List<PricingRecord> allRecords = pricingService.getAllRecords();
            for (int i = 0; i < allRecords.size(); i++) {
                if (allRecords.get(i).getInstrumentGuid() != null &&
                        allRecords.get(i).getInstrumentGuid().equals(guid)) {
                    targetIndex = i;
                    break;
                }
            }
        } else {
            // Multiple records found - show with actual indices from full list
            System.out.println("\n⚠ Multiple records found with GUID: " + guid + " (duplicates detected)");
            System.out.println("Please select by index from the full records list:");

            List<PricingRecord> allRecords = pricingService.getAllRecords();
            List<Integer> matchingIndices = new java.util.ArrayList<>();

            for (int i = 0; i < allRecords.size(); i++) {
                PricingRecord record = allRecords.get(i);
                if (record.getInstrumentGuid() != null && record.getInstrumentGuid().equals(guid)) {
                    matchingIndices.add(i);
                    System.out.println("  [Index " + i + "] " + formatRecordSummary(record));
                }
            }

            System.out.print("Enter index: ");
            try {
                int index = Integer.parseInt(scanner.nextLine().trim());
                if (matchingIndices.contains(index)) {
                    targetIndex = index;
                } else {
                    System.out.println("Invalid index. Please use one of the indices shown above.");
                    return;
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid index format.");
                return;
            }
        }

        PricingRecord update = new PricingRecord();

        System.out.println("\nEnter new values (press Enter to skip):");
        System.out.print("Price: ");
        String priceInput = scanner.nextLine().trim();
        if (!priceInput.isEmpty()) {
            try {
                update.setPrice(Double.parseDouble(priceInput));
            } catch (NumberFormatException e) {
                System.out.println("Invalid price format. Update cancelled.");
                return;
            }
        }

        System.out.print("Exchange (CME, NYMEX, CBOT, COMEX): ");
        String exchange = scanner.nextLine().trim();
        if (!exchange.isEmpty()) {
            update.setExchange(exchange);
        }

        System.out.print("Product Type (FUT, OPT): ");
        String productType = scanner.nextLine().trim();
        if (!productType.isEmpty()) {
            update.setProductType(productType);
        }

        System.out.print("Trade Date (YYYY-MM-DD): ");
        String dateInput = scanner.nextLine().trim();
        if (!dateInput.isEmpty()) {
            try {
                update.setTradeDate(java.time.LocalDate.parse(dateInput));
            } catch (Exception e) {
                System.out.println("Invalid date format. Update cancelled.");
                return;
            }
        }

        boolean success = pricingService.updateRecord(guid, update);
        if (success) {
            System.out.println("\n✓ Record updated successfully!");
            System.out.println("Record will be re-validated automatically.");
        } else {
            System.out.println("\n✗ Update failed. Please check the values and try again.");
        }
    }

    private void deleteRecord() {
        System.out.println("\n--- Delete Record ---");
        System.out.print("Enter Instrument GUID: ");
        String guid = scanner.nextLine().trim();

        if (guid.isEmpty()) {
            System.out.println("Error: GUID cannot be empty.");
            return;
        }

        List<PricingRecord> records = pricingService.getAllRecordsByGuid(guid);

        if (records.isEmpty()) {
            System.out.println("No record found with GUID: " + guid);
            return;
        }

        PricingRecord targetRecord;
        if (records.size() == 1) {
            targetRecord = records.get(0);
        } else {
            System.out.println("\nMultiple records found with GUID: " + guid);
            System.out.println("Please select by index:");
            for (int i = 0; i < records.size(); i++) {
                System.out.println("  [" + i + "] " + formatRecordSummary(records.get(i)));
            }
            System.out.print("Enter index: ");
            try {
                int index = Integer.parseInt(scanner.nextLine().trim());
                if (index >= 0 && index < records.size()) {
                    targetRecord = records.get(index);
                } else {
                    System.out.println("Invalid index.");
                    return;
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid index format.");
                return;
            }
        }

        System.out.println("\nRecord to delete:");
        displayRecord(targetRecord);
        System.out.print("\nAre you sure you want to delete this record? (yes/no): ");
        String confirm = scanner.nextLine().trim().toLowerCase();

        if (confirm.equals("yes") || confirm.equals("y")) {
            boolean success = pricingService.deleteRecord(guid);
            if (success) {
                System.out.println("\n✓ Record deleted successfully!");
            } else {
                System.out.println("\n✗ Delete failed.");
            }
        } else {
            System.out.println("Delete cancelled.");
        }
    }

    private void generateReportFile() {
        System.out.println("\n--- Generate Text Report File ---");

        ValidationReport report = pricingService.generateReport();

        if (report.getTotalRecords() == 0) {
            System.out.println("No data loaded. Please load a CSV file first (Option 1).");
            return;
        }

        try {
            String outputPath = "validation_report.txt";
            String filePath = reportGenerator.generateTextReport(report, outputPath);
            System.out.println("\n✓ Report generated successfully!");
            System.out.println("File saved to: " + filePath);
        } catch (IOException e) {
            System.out.println("Error: Failed to generate report file - " + e.getMessage());
        }
    }
}
