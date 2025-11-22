package com.cme.pricing.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;

/**
 * Model class representing the validation report
 */
public class ValidationReport {
    private int totalRecords;
    private int validRecords;
    private int invalidRecords;
    private int duplicateRecords;
    private int missingPriceRecords;
    private int invalidPriceFormatRecords;
    private int negativePriceRecords;
    private int zeroPriceRecords;
    private int missingInstrumentGuidRecords;
    private int missingTradeDateRecords;
    private int missingExchangeRecords;
    private int missingProductTypeRecords;
    private int invalidExchangeRecords;
    private int invalidProductTypeRecords;
    private List<PricingRecord> allRecords;
    private List<PricingRecord> invalidRecordsList;
    private List<String> duplicateRecordsList;

    public ValidationReport() {
        this.allRecords = new ArrayList<>();
        this.invalidRecordsList = new ArrayList<>();
        this.duplicateRecordsList = new ArrayList<>();
    }

    // Getters and Setters
    public int getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(int totalRecords) {
        this.totalRecords = totalRecords;
    }

    public int getValidRecords() {
        return validRecords;
    }

    public void setValidRecords(int validRecords) {
        this.validRecords = validRecords;
    }

    public int getInvalidRecords() {
        return invalidRecords;
    }

    public void setInvalidRecords(int invalidRecords) {
        this.invalidRecords = invalidRecords;
    }

    public int getDuplicateRecords() {
        return duplicateRecords;
    }

    public void setDuplicateRecords(int duplicateRecords) {
        this.duplicateRecords = duplicateRecords;
    }

    public int getMissingPriceRecords() {
        return missingPriceRecords;
    }

    public void setMissingPriceRecords(int missingPriceRecords) {
        this.missingPriceRecords = missingPriceRecords;
    }

    public int getInvalidPriceFormatRecords() {
        return invalidPriceFormatRecords;
    }

    public void setInvalidPriceFormatRecords(int invalidPriceFormatRecords) {
        this.invalidPriceFormatRecords = invalidPriceFormatRecords;
    }

    public List<PricingRecord> getAllRecords() {
        return allRecords;
    }

    public void setAllRecords(List<PricingRecord> allRecords) {
        this.allRecords = allRecords;
    }

    public List<PricingRecord> getInvalidRecordsList() {
        return invalidRecordsList;
    }

    public void setInvalidRecordsList(List<PricingRecord> invalidRecordsList) {
        this.invalidRecordsList = invalidRecordsList;
    }

    public List<String> getDuplicateRecordsList() {
        return duplicateRecordsList;
    }

    public void setDuplicateRecordsList(List<String> duplicateRecordsList) {
        this.duplicateRecordsList = duplicateRecordsList;
    }

    @JsonIgnore
    public int getNegativePriceRecords() {
        return negativePriceRecords;
    }

    public void setNegativePriceRecords(int negativePriceRecords) {
        this.negativePriceRecords = negativePriceRecords;
    }

    @JsonIgnore
    public int getZeroPriceRecords() {
        return zeroPriceRecords;
    }

    public void setZeroPriceRecords(int zeroPriceRecords) {
        this.zeroPriceRecords = zeroPriceRecords;
    }

    public int getMissingInstrumentGuidRecords() {
        return missingInstrumentGuidRecords;
    }

    public void setMissingInstrumentGuidRecords(int missingInstrumentGuidRecords) {
        this.missingInstrumentGuidRecords = missingInstrumentGuidRecords;
    }

    public int getMissingTradeDateRecords() {
        return missingTradeDateRecords;
    }

    public void setMissingTradeDateRecords(int missingTradeDateRecords) {
        this.missingTradeDateRecords = missingTradeDateRecords;
    }

    public int getMissingExchangeRecords() {
        return missingExchangeRecords;
    }

    public void setMissingExchangeRecords(int missingExchangeRecords) {
        this.missingExchangeRecords = missingExchangeRecords;
    }

    public int getMissingProductTypeRecords() {
        return missingProductTypeRecords;
    }

    public void setMissingProductTypeRecords(int missingProductTypeRecords) {
        this.missingProductTypeRecords = missingProductTypeRecords;
    }

    public int getInvalidExchangeRecords() {
        return invalidExchangeRecords;
    }

    public void setInvalidExchangeRecords(int invalidExchangeRecords) {
        this.invalidExchangeRecords = invalidExchangeRecords;
    }

    public int getInvalidProductTypeRecords() {
        return invalidProductTypeRecords;
    }

    public void setInvalidProductTypeRecords(int invalidProductTypeRecords) {
        this.invalidProductTypeRecords = invalidProductTypeRecords;
    }
}

