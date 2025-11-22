# API Commands Quick Reference

## Prerequisites

```powershell
# Make sure application is running
mvn spring-boot:run
```

---

## 1. Get API Info

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/pricing/" -Method Get | ConvertTo-Json
```

---

## 2. Load CSV Data

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/pricing/load" -Method Post -ContentType "application/json" -Body '{"filePath":"sample_data/pricing_data.csv"}' | ConvertTo-Json
```

---

## 3. Get Validation Report

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/pricing/report" -Method Get | ConvertTo-Json -Depth 10
```

---

## 4. Get All Records

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/pricing/records" -Method Get | ConvertTo-Json -Depth 5
```

---

## 5. Get Specific Record

```powershell
# Get single record
Invoke-RestMethod -Uri "http://localhost:8080/api/pricing/records/1001" -Method Get | ConvertTo-Json

# Get specific record from duplicates (use ?index parameter)
Invoke-RestMethod -Uri "http://localhost:8080/api/pricing/records/1004?index=3" -Method Get | ConvertTo-Json
```

---

## 6. Update Record (PUT) vs 7. Correct Record (POST)

**Quick Decision Guide:**

- **Only ONE record with that GUID?** → Use **UPDATE (PUT)** - simpler and works fine ✅
- **MULTIPLE records with same GUID?** → Use **CORRECT (POST)** - you can choose which one with `?index` ✅

---

## 6. Update Record (PUT)

```powershell
# Update single record
Invoke-RestMethod -Uri "http://localhost:8080/api/pricing/records/1003" -Method Put -ContentType "application/json" -Body '{"price":200.00}' | ConvertTo-Json

# Update specific record from duplicates (use ?index parameter)
Invoke-RestMethod -Uri "http://localhost:8080/api/pricing/records/1004?index=3" -Method Put -ContentType "application/json" -Body '{"price":450.00}' | ConvertTo-Json
```

---

## 7. Correct Record (POST)

**Use this when there are MULTIPLE records with the same GUID (duplicates)**

### Simple Case: Only ONE record with this GUID

```powershell
# Fix invalid price format
Invoke-RestMethod -Uri "http://localhost:8080/api/pricing/records/1005/correct" -Method Post -ContentType "application/json" -Body '{"price":275.50}' | ConvertTo-Json

# Fix multiple fields at once
Invoke-RestMethod -Uri "http://localhost:8080/api/pricing/records/1005/correct" -Method Post -ContentType "application/json" -Body '{"price":275.50,"exchange":"CME"}' | ConvertTo-Json
```

**When to use CORRECT:**

- ✅ You have MULTIPLE records with the same GUID (duplicates) and need to choose which one
- ✅ You want to know if the record became valid after correction (response shows `"recordIsNowValid": true`)

**Note:** For a single record, UPDATE and CORRECT work the same. Use UPDATE for simplicity. Use CORRECT only when you have duplicates.

---

### Multiple Records (Duplicates)

**When there are 2+ records with the same GUID, use ?index to specify which one:**

```powershell
# Correct specific record from duplicates (use ?index parameter)
Invoke-RestMethod -Uri "http://localhost:8080/api/pricing/records/1004/correct?index=4" -Method Post -ContentType "application/json" -Body '{"price":450.00,"exchange":"CME"}' | ConvertTo-Json
```

---

## 9. Delete Record (DELETE)

```powershell
# Delete single record
Invoke-RestMethod -Uri "http://localhost:8080/api/pricing/records/1006" -Method Delete | ConvertTo-Json

# Delete specific record from duplicates (use ?index parameter)
Invoke-RestMethod -Uri "http://localhost:8080/api/pricing/records/1004?index=4" -Method Delete | ConvertTo-Json
```

---

## 11. Generate Report File

```powershell
# Generate and view report
Invoke-RestMethod -Uri "http://localhost:8080/api/pricing/report/generate" -Method Post -ContentType "application/json" -Body '{"outputPath":"validation_report.txt"}' | Out-Null; Get-Content validation_report.txt

# Generate only (returns summary)
Invoke-RestMethod -Uri "http://localhost:8080/api/pricing/report/generate" -Method Post -ContentType "application/json" -Body '{"outputPath":"validation_report.txt"}' | ConvertTo-Json
```

---

## 12. Get Updated Report

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/pricing/report" -Method Get | ConvertTo-Json -Depth 10
```

---

## Quick Test Sequence

Run these in order:

```powershell
# 1. Load data
Invoke-RestMethod -Uri "http://localhost:8080/api/pricing/load" -Method Post -ContentType "application/json" -Body '{"filePath":"sample_data/pricing_data.csv"}' | ConvertTo-Json

# 2. View report
Invoke-RestMethod -Uri "http://localhost:8080/api/pricing/report" -Method Get | ConvertTo-Json -Depth 10

# 3. View all records
Invoke-RestMethod -Uri "http://localhost:8080/api/pricing/records" -Method Get | ConvertTo-Json -Depth 5

# 4. View specific record
Invoke-RestMethod -Uri "http://localhost:8080/api/pricing/records/1001" -Method Get | ConvertTo-Json

# 5. Generate report file
Invoke-RestMethod -Uri "http://localhost:8080/api/pricing/report/generate" -Method Post -ContentType "application/json" -Body '{"outputPath":"validation_report.txt"}' | Out-Null; Get-Content validation_report.txt
```

---

## Error Testing

```powershell
# Test invalid price (should fail)
Invoke-RestMethod -Uri "http://localhost:8080/api/pricing/records/1001" -Method Put -ContentType "application/json" -Body '{"price":-10.00}' | ConvertTo-Json

# Test zero price (should fail)
Invoke-RestMethod -Uri "http://localhost:8080/api/pricing/records/1001" -Method Put -ContentType "application/json" -Body '{"price":0}' | ConvertTo-Json

# Test record not found
Invoke-RestMethod -Uri "http://localhost:8080/api/pricing/records/9999" -Method Get | ConvertTo-Json
```

---

## Notes

- **Depth Parameter**: Use `-Depth 10` for complex nested responses (reports), `-Depth 5` for arrays of objects (records list)
- **Handling Duplicates**: When multiple records exist with the same GUID, use the `?index={index}` parameter to access a specific record
- **Update vs Correct - When to Use Which?**
  - **For a SINGLE record**: Both UPDATE and CORRECT work the same way. Use UPDATE for simplicity.
  - **For MULTIPLE records with same GUID**:
    - **UPDATE (PUT)**: Only updates the FIRST record (you can't choose which one) ❌
    - **CORRECT (POST)**: Can choose which record to update using `?index` parameter ✅
  - **Summary**: Use UPDATE for single records, use CORRECT when you have duplicates and need to choose which one
- **Price Validation**: Price must always be > 0 (negative or zero prices will be rejected)

---

## All Endpoints Summary

| Method | Endpoint                              | Purpose         | Depth   |
| ------ | ------------------------------------- | --------------- | ------- |
| GET    | `/api/pricing/`                       | API info        | Default |
| POST   | `/api/pricing/load`                   | Load CSV        | Default |
| GET    | `/api/pricing/report`                 | Get report      | 10      |
| POST   | `/api/pricing/report/generate`        | Generate file   | Default |
| GET    | `/api/pricing/records`                | All records     | 5       |
| GET    | `/api/pricing/records/{guid}`         | Specific record | Default |
| PUT    | `/api/pricing/records/{guid}`         | Update record   | Default |
| POST   | `/api/pricing/records/{guid}/correct` | Correct record  | Default |
| DELETE | `/api/pricing/records/{guid}`         | Delete record   | Default |
