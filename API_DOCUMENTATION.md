# API Documentation

## Pricing Data Validation & Reporting Utility - REST API

Base URL: `http://localhost:8080/api/pricing`

All endpoints return JSON responses.

---

## 1. Load and Validate Data

Loads pricing data from a CSV file and validates all records.

**Endpoint:** `POST /api/pricing/load`

**Request Body:**
```json
{
  "filePath": "sample_data/pricing_data.csv"
}
```

**Parameters:**
- `filePath` (string, required): Path to the CSV file relative to project root

**Response:**
```json
{
  "totalRecords": 6,
  "validRecords": 2,
  "invalidRecords": 4,
  "duplicateRecords": 1,
  "missingPriceRecords": 1,
  "invalidPriceFormatRecords": 1,
  "allRecords": [
    {
      "instrumentGuid": "1001",
      "tradeDate": "2025-01-10",
      "price": 123.45,
      "exchange": "CME",
      "productType": "FUT",
      "valid": true,
      "validationError": null
    },
    ...
  ],
  "invalidRecordsList": [...],
  "duplicateRecordsList": ["1004 - 2025-01-10"]
}
```

**Status Codes:**
- `200 OK`: Data loaded and validated successfully
- `400 Bad Request`: Invalid file path or unsupported format
- `500 Internal Server Error`: File read error or parsing failure

**Example (curl):**
```bash
curl -X POST http://localhost:8080/api/pricing/load \
  -H "Content-Type: application/json" \
  -d '{"filePath":"sample_data/pricing_data.csv"}'
```

---

## 2. Get Validation Report

Retrieves the current validation report without regenerating it.

**Endpoint:** `GET /api/pricing/report`

**Response:**
Same structure as the load endpoint response.

**Status Codes:**
- `200 OK`: Report retrieved successfully

**Example (curl):**
```bash
curl http://localhost:8080/api/pricing/report
```

---

## 3. Generate Text Report

Generates a formatted text report and saves it to a file.

**Endpoint:** `POST /api/pricing/report/generate`

**Request Body:**
```json
{
  "outputPath": "validation_report.txt"
}
```

**Parameters:**
- `outputPath` (string, optional): Path where the report will be saved. Default: "validation_report.txt"

**Response:**
```json
{
  "message": "Report generated successfully",
  "outputPath": "validation_report.txt",
  "report": "=== PRICING DATA VALIDATION REPORT ===\n..."
}
```

**Status Codes:**
- `200 OK`: Report generated successfully
- `500 Internal Server Error`: File write error

**Example (curl):**
```bash
curl -X POST http://localhost:8080/api/pricing/report/generate \
  -H "Content-Type: application/json" \
  -d '{"outputPath":"validation_report.txt"}'
```

---

## 4. Get All Records

Retrieves all pricing records currently loaded in the system.

**Endpoint:** `GET /api/pricing/records`

**Response:**
```json
[
  {
    "instrumentGuid": "1001",
    "tradeDate": "2025-01-10",
    "price": 123.45,
    "exchange": "CME",
    "productType": "FUT",
    "valid": true,
    "validationError": null
  },
  ...
]
```

**Status Codes:**
- `200 OK`: Records retrieved successfully

**Example (curl):**
```bash
curl http://localhost:8080/api/pricing/records
```

---

## 5. Get Specific Record

Retrieves a specific pricing record by instrument GUID.

**Endpoint:** `GET /api/pricing/records/{instrumentGuid}`

**Path Parameters:**
- `instrumentGuid` (string, required): The instrument GUID to retrieve

**Response (Success):**
```json
{
  "instrumentGuid": "1001",
  "tradeDate": "2025-01-10",
  "price": 123.45,
  "exchange": "CME",
  "productType": "FUT",
  "valid": true,
  "validationError": null
}
```

**Response (Not Found):**
```json
{
  "error": "Record not found for instrument GUID: 1001"
}
```

**Status Codes:**
- `200 OK`: Record found
- `404 Not Found`: Record not found

**Example (curl):**
```bash
curl http://localhost:8080/api/pricing/records/1001
```

---

## 6. Update Record

Updates an existing pricing record. All fields can be updated.

**Endpoint:** `PUT /api/pricing/records/{instrumentGuid}`

**Path Parameters:**
- `instrumentGuid` (string, required): The instrument GUID to update

**Request Body:**
```json
{
  "price": 150.00,
  "exchange": "CME",
  "productType": "FUT",
  "tradeDate": "2025-01-10"
}
```

**Parameters (all optional, but at least one should be provided):**
- `price` (number): New price value
- `exchange` (string): New exchange value
- `productType` (string): New product type
- `tradeDate` (string): New trade date in format "yyyy-MM-dd"

**Response (Success):**
```json
{
  "message": "Record updated successfully",
  "record": {
    "instrumentGuid": "1003",
    "tradeDate": "2025-01-10",
    "price": 150.00,
    "exchange": "CME",
    "productType": "FUT",
    "valid": true,
    "validationError": null
  }
}
```

**Response (Not Found):**
```json
{
  "error": "Record not found for instrument GUID: 1003"
}
```

**Status Codes:**
- `200 OK`: Record updated successfully
- `404 Not Found`: Record not found

**Example (curl):**
```bash
curl -X PUT http://localhost:8080/api/pricing/records/1003 \
  -H "Content-Type: application/json" \
  -d '{
    "price": 150.00,
    "exchange": "CME",
    "productType": "FUT",
    "tradeDate": "2025-01-10"
  }'
```

**Note:** After update, the record is automatically re-validated.

---

## 7. Delete Record

Deletes a pricing record from the system.

**Endpoint:** `DELETE /api/pricing/records/{instrumentGuid}`

**Path Parameters:**
- `instrumentGuid` (string, required): The instrument GUID to delete

**Response (Success):**
```json
{
  "message": "Record deleted successfully"
}
```

**Response (Not Found):**
```json
{
  "error": "Record not found for instrument GUID: 1005"
}
```

**Status Codes:**
- `200 OK`: Record deleted successfully
- `404 Not Found`: Record not found

**Example (curl):**
```bash
curl -X DELETE http://localhost:8080/api/pricing/records/1005
```

---

## 8. Correct Invalid Record

Corrects an invalid record by updating specific fields. This is similar to update but specifically designed for fixing validation errors.

**Endpoint:** `POST /api/pricing/records/{instrumentGuid}/correct`

**Path Parameters:**
- `instrumentGuid` (string, required): The instrument GUID to correct

**Request Body:**
```json
{
  "price": 150.00
}
```

**Parameters (all optional, but at least one should be provided):**
- `price` (number): Corrected price value
- `exchange` (string): Corrected exchange value
- `productType` (string): Corrected product type
- `tradeDate` (string): Corrected trade date in format "yyyy-MM-dd"

**Response (Success):**
```json
{
  "message": "Record corrected successfully",
  "record": {
    "instrumentGuid": "1003",
    "tradeDate": "2025-01-10",
    "price": 150.00,
    "exchange": "CME",
    "productType": "FUT",
    "valid": true,
    "validationError": null
  },
  "isValid": true
}
```

**Response (Not Found):**
```json
{
  "error": "Record not found for instrument GUID: 1003"
}
```

**Status Codes:**
- `200 OK`: Record corrected successfully
- `404 Not Found`: Record not found

**Example (curl):**
```bash
curl -X POST http://localhost:8080/api/pricing/records/1003/correct \
  -H "Content-Type: application/json" \
  -d '{"price": 150.00}'
```

**Note:** After correction, the record is automatically re-validated and the validation report is regenerated.

---

## Data Models

### PricingRecord

```json
{
  "instrumentGuid": "string",
  "tradeDate": "yyyy-MM-dd",
  "price": number,
  "exchange": "string",
  "productType": "string",
  "valid": boolean,
  "validationError": "string | null"
}
```

### ValidationReport

```json
{
  "totalRecords": number,
  "validRecords": number,
  "invalidRecords": number,
  "duplicateRecords": number,
  "missingPriceRecords": number,
  "invalidPriceFormatRecords": number,
  "allRecords": [PricingRecord],
  "invalidRecordsList": [PricingRecord],
  "duplicateRecordsList": [string]
}
```

---

## Error Responses

All error responses follow this format:

```json
{
  "error": "Error message description"
}
```

Common HTTP status codes:
- `400 Bad Request`: Invalid request parameters
- `404 Not Found`: Resource not found
- `500 Internal Server Error`: Server-side error

---

## Usage Examples

### Complete Workflow

1. **Load data:**
```bash
curl -X POST http://localhost:8080/api/pricing/load \
  -H "Content-Type: application/json" \
  -d '{"filePath":"sample_data/pricing_data.csv"}'
```

2. **View report:**
```bash
curl http://localhost:8080/api/pricing/report
```

3. **Correct invalid record:**
```bash
curl -X POST http://localhost:8080/api/pricing/records/1003/correct \
  -H "Content-Type: application/json" \
  -d '{"price": 150.00}'
```

4. **Generate final report:**
```bash
curl -X POST http://localhost:8080/api/pricing/report/generate \
  -H "Content-Type: application/json" \
  -d '{"outputPath":"final_report.txt"}'
```

---

## Testing with Postman

1. Import the following collection or create requests manually:

**Collection: Pricing Validation API**

- POST `http://localhost:8080/api/pricing/load`
  - Body (raw JSON): `{"filePath":"sample_data/pricing_data.csv"}`

- GET `http://localhost:8080/api/pricing/report`

- GET `http://localhost:8080/api/pricing/records`

- GET `http://localhost:8080/api/pricing/records/1001`

- PUT `http://localhost:8080/api/pricing/records/1003`
  - Body (raw JSON): `{"price":150.00,"exchange":"CME","productType":"FUT","tradeDate":"2025-01-10"}`

- POST `http://localhost:8080/api/pricing/records/1003/correct`
  - Body (raw JSON): `{"price":150.00}`

- DELETE `http://localhost:8080/api/pricing/records/1005`

---

## Notes

- All dates must be in `yyyy-MM-dd` format
- Prices must be positive numbers
- Valid exchanges: CME, NYMEX, CBOT, COMEX
- Valid product types: FUT, OPT
- The application maintains state in memory (data is lost on restart)
- File paths can be relative to project root or absolute paths

