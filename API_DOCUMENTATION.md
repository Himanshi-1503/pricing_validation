# API Documentation

## Pricing Data Validation & Reporting Utility - REST API

**Base URL:**

- `https://pricing-validation-iab5.onrender.com/api/pricing`

All endpoints return JSON responses.

---

## 1. Get API Information

Retrieves API information including available endpoints and application status.

**Endpoint:** `GET /api/pricing/`

**Response:**

```json
{
  "application": "Pricing Data Validation & Reporting Utility",
  "version": "1.0.0",
  "status": "running",
  "endpoints": {
    "load": "POST /api/pricing/load",
    "report": "GET /api/pricing/report",
    "generateReport": "POST /api/pricing/report/generate",
    "allRecords": "GET /api/pricing/records",
    "getSpecificRecord": "GET /api/pricing/records/{instrumentGuid}",
    "updateRecord": "PUT /api/pricing/records/{instrumentGuid}",
    "deleteRecord": "DELETE /api/pricing/records/{instrumentGuid}",
    "updateSpecificRecord": "POST /api/pricing/records/{instrumentGuid}/correct"
  }
}
```

**Status Codes:**

- `200 OK`: API information retrieved successfully

---

## 2. Load and Validate Data

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
  "message": "Data loaded and validated successfully!",
  "totalRecords": 21,
  "validRecords": 11,
  "invalidRecords": 10,
  "duplicateRecords": 1,
  "missingValues": 6
}
```

**Status Codes:**

- `200 OK`: Data loaded and validated successfully
- `400 Bad Request`: Invalid file path or unsupported format
- `500 Internal Server Error`: File read error or parsing failure

---

## 3. Get Validation Report

Retrieves the current validation report without regenerating it.

**Endpoint:** `GET /api/pricing/report`

**Response:**

```json
{
  "totalRecords": 21,
  "validRecords": 11,
  "invalidRecords": 10,
  "duplicateRecords": 1,
  "missingValues": 6,
  "errorBreakdown": {
    "Missing Price": 1,
    "Invalid Price Format": 2,
    "Missing instrument_guid": 2,
    "Missing trade_date": 1,
    "Missing exchange": 1,
    "Missing product_type": 1,
    "Invalid exchange": 1,
    "Invalid product_type": 1,
    "Duplicate Records": 1
  },
  "invalidRecordsList": [
    {
      "guid": "1003",
      "tradeDate": "2025-01-10",
      "price": "",
      "exchange": "CME",
      "productType": "FUT",
      "error": "Missing price value"
    }
    // ... 9 more invalid records
  ],
  "duplicateRecordsList": [
    {
      "guid": "1004",
      "tradeDate": "2025-01-10",
      "price": 350.0,
      "exchange": "CBOT",
      "productType": "FUT",
      "error": "Duplicate GUID (primary key violation)"
    }
  ],
  "missingValuesDetails": {
    "missingPrice": [
      {
        "guid": "1003",
        "tradeDate": "2025-01-10",
        "price": "",
        "exchange": "CME",
        "productType": "FUT",
        "error": "Missing price value"
      }
    ]
    // ... 5 more record
  },
  "allRecordsTable": [
    {
      "guid": "1001",
      "date": "2025-01-10",
      "price": 123.45,
      "exchange": "CME",
      "product": "FUT",
      "status": "VALID"
    },
    {
      "guid": "1002",
      "date": "2025-01-10",
      "price": 222.1,
      "exchange": "NYMEX",
      "product": "OPT",
      "status": "VALID"
    },
    {
      "guid": "1003",
      "date": "2025-01-10",
      "price": "",
      "exchange": "CME",
      "product": "FUT",
      "status": "INVALID"
    }
    // ... 18 more records
  ]
}
```

**Note:** The response includes complete details for all records. Only a few examples are shown above for brevity.

**Status Codes:**

- `200 OK`: Report retrieved successfully

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
  ...// All records
]
```

**Status Codes:**

- `200 OK`: Records retrieved successfully

---

## 5. Get Specific Record

Retrieves a specific pricing record by instrument GUID.

**Endpoint:** `GET /api/pricing/records/{instrumentGuid}`

**Path Parameters:**

- `instrumentGuid` (string, required): The instrument GUID to retrieve

**Response (Success - Single Record):**

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

**Response (Multiple Records Found - Duplicates or Empty GUIDs):**

When multiple records share the same GUID (duplicates) or when accessing empty GUIDs, the API returns a list with indices:

```json
{
  "records": [
    {
      "index": 3,
      "instrumentGuid": "1004",
      "tradeDate": "2025-01-10",
      "price": 350.0,
      "exchange": "CBOT",
      "productType": "FUT",
      "status": "VALID",
      "error": null
    },
    {
      "index": 4,
      "instrumentGuid": "1004",
      "tradeDate": "2025-01-10",
      "price": 350.0,
      "exchange": "CBOT",
      "productType": "FUT",
      "status": "INVALID",
      "error": "Duplicate GUID (primary key violation)"
    }
  ],
  "count": 2,
  "message": "Multiple records found with GUID: 1004",
  "instruction": "Use GET /api/pricing/records/1004?index={index} to view a specific record"
}

//using indexes
{
    "index": 4,
    "instrumentGuid": "1004",
    "tradeDate": "2025-01-10",
    "price": 350.0,
    "exchange": "CBOT",
    "productType": "FUT",
    "valid": false,
    "validationError": "Duplicate GUID (primary key violation)"
}
```

**Response (Empty GUID Example):**

```json
{
  "records": [
    {
      "index": 13,
      "instrumentGuid": "",
      "tradeDate": "2025-01-10",
      "price": 200.0,
      "exchange": "",
      "productType": "FUT",
      "status": "INVALID",
      "error": "Missing exchange; Missing instrument GUID (primary key required)"
    },
    {
      "index": 16,
      "instrumentGuid": "",
      "tradeDate": "2025-01-10",
      "price": 250.0,
      "exchange": "COMEX",
      "productType": "OPT",
      "status": "INVALID",
      "error": "Missing instrument GUID (primary key required)"
    }
  ],
  "count": 2,
  "message": "Multiple records found with GUID: (empty)",
  "instruction": "Use GET /api/pricing/records/EMPTY?index={index} to view a specific record"
}
  // use index after getting indexes
{
    "index": 13,
    "instrumentGuid": "",
    "tradeDate": "2025-01-10",
    "price": 200.0,
    "exchange": "",
    "productType": "FUT",
    "valid": false,
    "validationError": "Missing exchange; Missing instrument GUID (primary key required)"
}
```

**Response (Not Found):**

```json
{
  "error": "Record not found for instrument GUID: 1001"
}
```

**Status Codes:**

- `200 OK`: Record found (single or multiple)
- `404 Not Found`: Record not found
- `409 Conflict`: Multiple records found (when accessing without index parameter)

**Query Parameters:**

- `index` (integer, optional): Specify which record to retrieve when multiple records share the same GUID

**Note:** When accessing records with duplicate GUIDs or empty GUIDs:

1. **First request (without index):** The API will return an error response with a list of all matching records and their indices
2. **Second request (with index):** Use the `?index=` parameter with one of the provided indices to retrieve the specific record

## 6. Update Record

Updates an existing pricing record. All fields can be updated.

**Endpoint:** `PUT /api/pricing/records/{instrumentGuid}`

**Path Parameters:**

- `instrumentGuid` (string, required): The instrument GUID to update

**Request Body:**

```json
{
  "price": 150.0,
  "exchange": "CME",
  "productType": "FUT",
  "tradeDate": "2025-01-10"
}
```

**Response (Success):**

```json
{
  "message": "Record updated successfully",
  "record": {
    "instrumentGuid": "1003",
    "tradeDate": "2025-01-10",
    "price": 150.0,
    "exchange": "CME",
    "productType": "FUT",
    "valid": true,
    "validationError": null
  }
}
```

**Response (Multiple Records Found - Step 1):**

When multiple records share the same GUID or when accessing empty GUIDs without the index parameter, the API first returns a list with indices:

**Step 2 - With index parameter (returns success):**

```
**Example** PUT /api/pricing/records/1004?index=3
```

Returns the success response shown above.

**Status Codes:**

- `200 OK`: Record updated successfully
- `400 Bad Request`: Multiple records found - index parameter required
- `404 Not Found`: Record not found
- `409 Conflict`: Multiple records found (when accessing without index parameter)

**Query Parameters:**

- `index` (integer, optional): Specify which record to update when multiple records share the same GUID

**Note:**

- After update, the record is automatically re-validated.
- **For duplicates and null GUIDs:** When performing an update operation without the `?index=` parameter, the API will first return an error response listing all matching records with their indices. Use one of these indices in the `?index=` parameter to update the specific record (e.g., `PUT /api/pricing/records/1004?index=3`).
- For records with duplicate GUIDs, use the `?index=` parameter (e.g., `PUT /api/pricing/records/1004?index=3`)
- For empty GUIDs, use `EMPTY` as the GUID placeholder (e.g., `PUT /api/pricing/records/EMPTY?index=14`)

---

## 7. Correct Invalid Record

Corrects an invalid record by updating specific fields. This is similar to update but specifically designed for fixing validation errors.

**Endpoint:** `POST /api/pricing/records/{instrumentGuid}/correct`

**Path Parameters:**

- `instrumentGuid` (string, required): The instrument GUID to correct

**Request Body:**

```json
{
  "price": 150.0
}
```

**Response (Success):**

```json
{
  "message": "Record corrected successfully",
  "record": {
    "instrumentGuid": "1003",
    "tradeDate": "2025-01-10",
    "price": 150.0,
    "exchange": "CME",
    "productType": "FUT",
    "valid": true,
    "validationError": null
  },
  "isValid": true
}
```

**Response (Multiple Records Found - Step 1):**

When multiple records share the same GUID or when accessing empty GUIDs without the index parameter, the API first returns a list with indices:

**Step 2 - With index parameter (returns success):**

````
Example- POST /api/pricing/records/1004/correct?index=4
```json
{
  "price": 150.0
}```
Returns the success response shown above.

**Response (Not Found):**

```json
{
  "error": "Record not found for instrument GUID: 1003"
}
````

**Status Codes:**

- `200 OK`: Record corrected successfully
- `400 Bad Request`: Multiple records found - index parameter required, or correction failed
- `404 Not Found`: Record not found
- `409 Conflict`: Multiple records found (when accessing without index parameter)

**Query Parameters:**

- `index` (integer, optional): Specify which record to correct when multiple records share the same GUID

**Note:**

- After correction, the record is automatically re-validated and the validation report is regenerated.
- **For duplicates and null GUIDs:** When performing a correct operation without the `?index=` parameter, the API will first return an error response listing all matching records with their indices. Use one of these indices in the `?index=` parameter to correct the specific record (e.g., `POST /api/pricing/records/1004/correct?index=3`).
- For records with duplicate GUIDs, use the `?index=` parameter (e.g., `POST /api/pricing/records/1004/correct?index=3`)
- For empty GUIDs, use `EMPTY` as the GUID placeholder (e.g., `POST /api/pricing/records/EMPTY/correct?index=14`)

**Example - Correcting Empty GUID Record:**

To correct a record with an empty GUID, use `EMPTY` as the GUID placeholder:

**Request:**

```
POST /api/pricing/records/EMPTY/correct?index=13
Content-Type: application/json

{
  "instrumentGuid": "1021",
  "price": 200.0,
  "exchange": "CME",
  "productType": "FUT",
  "tradeDate": "2025-01-10"
}
```

**Response:**

```json
{
  "message": "Record corrected successfully",
  "record": {
    "instrumentGuid": "1021",
    "tradeDate": "2025-01-10",
    "price": 200.0,
    "exchange": "CME",
    "productType": "FUT",
    "valid": true,
    "validationError": null
  },
  "isValid": true
}
```

**Note:** When correcting an empty GUID record, you can add a GUID value in the request body to assign a new GUID to the record.

---

## 8. Delete Record

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

**Response (Multiple Records Found - Step 1):**

When multiple records share the same GUID or when accessing empty GUIDs without the index parameter, the API first returns a list with indices:

```

**Step 2 - With index parameter (returns success):**

```

DELETE /api/pricing/records/1004?index=4

````

Returns the success response shown above.

**Response (Not Found):**

```json
{
  "error": "Record not found for instrument GUID: 1005"
}
````

**Status Codes:**

- `200 OK`: Record deleted successfully
- `400 Bad Request`: Multiple records found - index parameter required
- `404 Not Found`: Record not found
- `409 Conflict`: Multiple records found (when accessing without index parameter)

**Query Parameters:**

- `index` (integer, optional): Specify which record to delete when multiple records share the same GUID

**Note:**

- **For duplicates and null GUIDs:** When performing a delete operation without the `?index=` parameter, the API will first return an error response listing all matching records with their indices. Use one of these indices in the `?index=` parameter to delete the specific record (e.g., `DELETE /api/pricing/records/1004?index=4`).
- For records with duplicate GUIDs, use the `?index=` parameter (e.g., `DELETE /api/pricing/records/1004?index=4`)
- For empty GUIDs, use `EMPTY` as the GUID placeholder (e.g., `DELETE /api/pricing/records/EMPTY?index=14`)

---

## 9. Generate Text Report

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

## Usage

### Typical Workflow

1. **Get API Info:** `GET /api/pricing/` - Check API status and available endpoints
2. **Load Data:** `POST /api/pricing/load` - Load and validate CSV file
3. **View Report:** `GET /api/pricing/report` - Get validation summary and details
4. **View Records:** `GET /api/pricing/records` - List all records with indices
5. **Update Records:** `PUT /api/pricing/records/{guid}` - Update record fields
6. **Correct Errors:** `POST /api/pricing/records/{guid}/correct` - Fix invalid records
7. **Delete Records:** `DELETE /api/pricing/records/{guid}` - Remove records
8. **Generate Report:** `POST /api/pricing/report/generate` - Create text report file (final step)

### Handling Duplicates and Empty GUIDs

**Important:** When performing operations (GET SPECIFIC RECORD,UPDATE,CORRECT,DELETE) on records with duplicate GUIDs or empty GUIDs:

1. **First Request (without index):** The API will return an error response containing a list of all matching records with their indices
2. **Second Request (with index):** Use the `?index=` parameter with one of the provided indices to perform the operation on the specific record

**Examples:**

- **Duplicate GUIDs:** When multiple records share the same GUID, use the `?index=` parameter to target a specific record (e.g., `GET /api/pricing/records/1004?index=3`)
- **Empty GUIDs:** Use `EMPTY` as the GUID placeholder (e.g., `GET /api/pricing/records/EMPTY?index=14`)

## Postman Testing

### Postman Collection

**Base URL:** `https://pricing-validation-iab5.onrender.com/api/pricing`

#### 1. Get API Info

- **Method**: GET
- **URL**: `https://pricing-validation-iab5.onrender.com/api/pricing/`
- **Body**: None

#### 2. Load Data

- **Method**: POST
- **URL**: `https://pricing-validation-iab5.onrender.com/api/pricing/load`
- **Headers**: `Content-Type: application/json`
- **Body** (raw JSON):
  ```json
  {
    "filePath": "sample_data/pricing_data.csv"
  }
  ```

#### 3. Get Validation Report

- **Method**: GET
- **URL**: `https://pricing-validation-iab5.onrender.com/api/pricing/report`
- **Body**: None

#### 4. Get All Records

- **Method**: GET
- **URL**: `https://pricing-validation-iab5.onrender.com/api/pricing/records`
- **Body**: None

#### 5. Get Specific Record

- **Method**: GET
- **URL**: `https://pricing-validation-iab5.onrender.com/api/pricing/records/1001`
- **Body**: None
- **Note**: For duplicates, use `?index=3` parameter. Example: `https://pricing-validation-iab5.onrender.com/api/pricing/records/1004?index=3` and for Null Guid, use `https://pricing-validation-iab5.onrender.com/api/pricing/records/EMPTY?index=13`

#### 6. Update Record

- **Method**: PUT
- **URL**: `https://pricing-validation-iab5.onrender.com/api/pricing/records/1003`
- **Headers**: `Content-Type: application/json`
- **Body** (raw JSON):
  ```json
  {
    "price": 150.0
  }
  ```
- **Note**: For duplicates, use `?index=3` parameter. Example: `https://pricing-validation-iab5.onrender.com/api/pricing/records/1004?index=3` and for Null Guid, use `https://pricing-validation-iab5.onrender.com/api/pricing/records/EMPTY?index=13`

#### 7. Correct Invalid Record

- **Method**: POST
- **URL**: `https://pricing-validation-iab5.onrender.com/api/pricing/records/1005/correct`
- **Headers**: `Content-Type: application/json`
- **Body** (raw JSON):
  ```json
  {
    "price": 175.0
  }
  ```
- **Note**: For duplicates, use `?index=3` parameter. Example: `https://pricing-validation-iab5.onrender.com/api/pricing/records/1004/correct?index=4` and for Null Guid, use `https://pricing-validation-iab5.onrender.com/api/pricing/records/EMPTY/correct?index=13`

#### 8. Delete Record

- **Method**: DELETE
- **URL**: `https://pricing-validation-iab5.onrender.com/api/pricing/records/1020`
- **Body**: None
- **Note**: For duplicates, use `?index=3` parameter. Example: `https://pricing-validation-iab5.onrender.com/api/pricing/records/1004?index=3` and for Null Guid, use `https://pricing-validation-iab5.onrender.com/api/pricing/records/EMPTY?index=13`

#### 9. Generate Text Report

- **Method**: POST
- **URL**: `https://pricing-validation-iab5.onrender.com/api/pricing/report/generate`
- **Headers**: `Content-Type: application/json`
- **Body** (raw JSON):
  ```json
  {
    "outputPath": "validation_report.txt"
  }
  ```

### Testing Tips

- **For Duplicate GUIDs**: Add `?index=` parameter (e.g., `https://pricing-validation-iab5.onrender.com/api/pricing/records/1004?index=3`)
- **For Empty GUIDs**: Use `EMPTY` as placeholder (e.g., `https://pricing-validation-iab5.onrender.com/api/pricing/records/EMPTY?index=16`)
- **Check Response**: All responses are JSON format
- **Error Handling**: Check status codes and error messages in response body

## Notes

- All dates must be in `yyyy-MM-dd` format
- Prices must be positive numbers (> 0)
- Valid exchanges: CME, NYMEX, CBOT, COMEX
- Valid product types: FUT, OPT
- The application maintains state in memory (data is lost on restart)
- File paths can be relative to project root or absolute paths
- For detailed testing instructions and validation rules, see `TESTING_GUIDE.md`
