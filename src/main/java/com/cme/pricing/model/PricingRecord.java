package com.cme.pricing.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Model class representing a pricing record
 */
public class PricingRecord {
    private String instrumentGuid;
    private LocalDate tradeDate;
    private Double price;
    private String originalPriceValue; // Store original invalid price string
    private String exchange;
    private String productType;

    // Validation flags
    private boolean isValid = true;
    private String validationError;

    public PricingRecord() {
    }

    public PricingRecord(String instrumentGuid, LocalDate tradeDate, Double price,
            String exchange, String productType) {
        this.instrumentGuid = instrumentGuid;
        this.tradeDate = tradeDate;
        this.price = price;
        this.exchange = exchange;
        this.productType = productType;
    }

    // Getters and Setters
    public String getInstrumentGuid() {
        return instrumentGuid;
    }

    public void setInstrumentGuid(String instrumentGuid) {
        this.instrumentGuid = instrumentGuid;
    }

    public LocalDate getTradeDate() {
        return tradeDate;
    }

    public void setTradeDate(LocalDate tradeDate) {
        this.tradeDate = tradeDate;
    }

    @JsonIgnore
    public Double getPrice() {
        return price;
    }

    @JsonProperty("price")
    public void setPrice(Double price) {
        this.price = price;
    }

    public String getOriginalPriceValue() {
        return originalPriceValue;
    }

    public void setOriginalPriceValue(String originalPriceValue) {
        this.originalPriceValue = originalPriceValue;
    }

    /**
     * Gets the price for JSON serialization - returns original invalid value if
     * price is null and originalPriceValue is set
     */
    @JsonProperty("price")
    public Object getPriceForJson() {
        if (price != null) {
            return price;
        } else if (originalPriceValue != null && !originalPriceValue.isEmpty()) {
            return originalPriceValue;
        }
        return null;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean valid) {
        isValid = valid;
    }

    public String getValidationError() {
        return validationError;
    }

    public void setValidationError(String validationError) {
        this.validationError = validationError;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PricingRecord that = (PricingRecord) o;
        return Objects.equals(instrumentGuid, that.instrumentGuid) &&
                Objects.equals(tradeDate, that.tradeDate) &&
                Objects.equals(price, that.price) &&
                Objects.equals(exchange, that.exchange) &&
                Objects.equals(productType, that.productType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(instrumentGuid, tradeDate, price, exchange, productType);
    }

    @Override
    public String toString() {
        return "PricingRecord{" +
                "instrumentGuid='" + instrumentGuid + '\'' +
                ", tradeDate=" + tradeDate +
                ", price=" + price +
                ", exchange='" + exchange + '\'' +
                ", productType='" + productType + '\'' +
                ", isValid=" + isValid +
                ", validationError='" + validationError + '\'' +
                '}';
    }
}
