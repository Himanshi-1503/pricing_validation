package com.cme.pricing.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Home controller for root endpoint
 */
@RestController
public class HomeController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> home() {
        Map<String, Object> response = new HashMap<>();
        response.put("application", "Pricing Data Validation & Reporting Utility");
        response.put("version", "1.0.0");
        response.put("status", "running");
        response.put("message", "API is running successfully!");
        response.put("baseUrl", "/api/pricing");
        
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("apiInfo", "GET /api/pricing/");
        endpoints.put("load", "POST /api/pricing/load");
        endpoints.put("report", "GET /api/pricing/report");
        endpoints.put("generateReport", "POST /api/pricing/report/generate");
        endpoints.put("allRecords", "GET /api/pricing/records");
        endpoints.put("getSpecificRecord", "GET /api/pricing/records/{instrumentGuid}");
        endpoints.put("updateRecord", "PUT /api/pricing/records/{instrumentGuid}");
        endpoints.put("deleteRecord", "DELETE /api/pricing/records/{instrumentGuid}");
        endpoints.put("updateSpecificRecord", "POST /api/pricing/records/{instrumentGuid}/correct");
        
        response.put("endpoints", endpoints);
        response.put("documentation", "See README.md for detailed API documentation");
        
        return ResponseEntity.ok(response);
    }
}

