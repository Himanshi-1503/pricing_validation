# Testing Guide

This guide provides comprehensive instructions for testing the Pricing Data Validation & Reporting Utility, including validation rules, sample data explanation, CLI usage, and step-by-step testing procedures.

---

## Table of Contents

1. [Validation Rules]
2. [Sample Dataset]
3. [CLI Testing]
4. [API Testing]
5. [Step-by-Step Testing Workflow]
6. [Understanding Validation Reports]

---

## Validation Rules

The application enforces comprehensive validation rules to ensure data quality. Each record is validated against the following criteria:

### 1. Price Validation

**Requirements:**

- **Required**: Price field must be present (cannot be null or empty)
- **Format**: Must be a valid numeric value (decimal numbers allowed)
- **Range**: Must be greater than zero

**Accepted Values:**

- ✅ Positive decimal numbers (e.g., `150.00`, `99.99`, `123.45`)

**Rejected Values:**

- ❌ **Negative prices** (e.g., `-320.00`)
- ❌ **Zero prices** (`0`)
- ❌ **Non-numeric values** (e.g., `"INVALID"`, `"ABC123"`)
- ❌ **Null or empty values**

**Error Messages:**

- `"Missing price value"` - When price is null or empty
- `"Invalid price format: {value}"` - When price is not numeric
- `"Negative price"` - When price is less than zero
- `"Zero price"` - When price equals zero

---

### 2. Exchange Validation

**Requirements:**

- **Required**: Exchange field must be present and non-empty
- **Valid Values**: Must be one of the predefined exchanges (case-insensitive)

**Valid Exchanges:**

- `CME` (Chicago Mercantile Exchange)
- `NYMEX` (New York Mercantile Exchange)
- `CBOT` (Chicago Board of Trade)
- `COMEX` (Commodity Exchange)

**Rejected Values:**

- ❌ Empty strings
- ❌ Null values
- ❌ Invalid exchanges (e.g., `"INVALID"`, `"NASDAQ"`, `"NYSE"`)

**Error Messages:**

- `"Missing exchange"` - When exchange is null or empty
- `"Invalid exchange: {value}"` - When exchange is not in the valid list

---

### 3. Product Type Validation

**Requirements:**

- **Required**: Product type field must be present and non-empty
- **Valid Values**: Must be one of the predefined product types (case-insensitive)

**Valid Product Types:**

- `FUT` (Futures)
- `OPT` (Options)

**Rejected Values:**

- ❌ Empty strings
- ❌ Null values
- ❌ Invalid product types (e.g., `"INVALID"`, `"STOCK"`, `"BOND"`)

**Error Messages:**

- `"Missing product type"` - When product type is null or empty
- `"Invalid product type: {value}"` - When product type is not in the valid list

---

### 4. Instrument GUID Validation (Primary Key)

**Requirements:**

- **Required**: Instrument GUID is the **primary key** and must be present and non-empty
- **Uniqueness**: Each GUID must be unique across all records

**Behavior:**

- First occurrence of a GUID is considered valid (if all other validations pass)
- Subsequent occurrences with the same GUID are marked as **duplicate** and invalid
- Records with null or empty GUIDs are automatically invalid

**Rejected Values:**

- ❌ Null GUIDs
- ❌ Empty GUIDs
- ❌ Duplicate GUIDs (second occurrence and beyond)

**Error Messages:**

- `"Missing instrument GUID (primary key required)"` - When GUID is null or empty
- `"Duplicate GUID (primary key violation)"` - When GUID already exists in another record

**Duplicate Handling:**

- The **first record** with a GUID is marked as valid (if other validations pass)
- All **subsequent records** with the same GUID are marked as invalid with duplicate error
- Both records are tracked and reported in the validation report

---

### 5. Trade Date Validation

**Requirements:**

- **Required**: Trade date field must be present and non-empty
- **Format**: Must be in `YYYY-MM-DD` format (e.g., `"2025-01-10"`)

**Accepted Format:**

- ✅ `YYYY-MM-DD` format (e.g., `"2025-01-10"`, `"2024-12-31"`)

**Rejected Values:**

- ❌ Null values
- ❌ Empty strings
- ❌ Invalid date formats (e.g., `"01/10/2025"`, `"10-01-2025"`, `"INVALID"`)

**Error Messages:**

- `"Missing trade date"` - When trade date is null or empty
- Date parsing errors are logged but the record is marked invalid

---

### 6. Duplicate Detection

**Definition:**
Records with the same Instrument GUID are considered duplicates.

**Primary Key Enforcement:**
Since GUID is the primary key, duplicates violate data integrity.

**Handling Logic:**

1. **First Occurrence**: Valid (if all other validations pass)
2. **Subsequent Occurrences**: Marked as invalid with error `"Duplicate GUID (primary key violation)"`
3. **Tracking**: All duplicate records are tracked and reported separately in the validation report

**Example:**
If two records have GUID `1004`:

- Record at index 3 (row 5 in CSV): **VALID** (first occurrence)
- Record at index 4 (row 6 in CSV): **INVALID** (duplicate, error: "Duplicate GUID (primary key violation)")

---

## Sample Dataset

The project includes `sample_data/pricing_data.csv` with various validation scenarios to test all validation rules.

### Dataset Overview

The sample CSV contains **21 records** with the following test cases:

| GUID    | Status       | Issue                   | Description                                                       |
| ------- | ------------ | ----------------------- | ----------------------------------------------------------------- |
| 1001    | ✅ Valid     | None                    | Complete valid record (123.45, CME, FUT)                          |
| 1002    | ✅ Valid     | None                    | Complete valid record (222.10, NYMEX, OPT)                        |
| 1003    | ❌ Invalid   | Missing price           | Price field is empty                                              |
| 1004    | ⚠️ Duplicate | Duplicate GUID          | Appears twice (index 4 & 5) - first is valid, second is duplicate |
| 1005    | ❌ Invalid   | Invalid price format    | Price = "INVALID"                                                 |
| 1006    | ✅ Valid     | None                    | Complete valid record (275.50, CME, OPT)                          |
| 1007    | ✅ Valid     | None                    | Complete valid record (450.75, NYMEX, FUT)                        |
| 1008    | ✅ Valid     | None                    | Complete valid record (180.25, CBOT, OPT)                         |
| 1009    | ✅ Valid     | None                    | Complete valid record (50.00, COMEX, FUT)                         |
| 1010    | ✅ Valid     | None                    | Complete valid record (150.00, CME, FUT)                          |
| 1011    | ❌ Invalid   | Invalid exchange        | Exchange = "INVALID"                                              |
| 1012    | ❌ Invalid   | Invalid product type    | Product type = "INVALID"                                          |
| (empty) | ❌ Invalid   | Missing GUID & exchange | GUID and exchange fields are empty (index 15)                     |
| 1014    | ❌ Invalid   | Missing product type    | Product type field is empty                                       |
| 1015    | ✅ Valid     | None                    | Complete valid record (300.00, CBOT, FUT)                         |
| (empty) | ❌ Invalid   | Missing GUID            | GUID field is empty (index 18)                                    |
| 1017    | ❌ Invalid   | Missing trade date      | Trade date field is empty                                         |
| 1018    | ❌ Invalid   | Invalid price format    | Price = "ABC123"                                                  |
| 1019    | ✅ Valid     | None                    | Complete valid record (500.00, CBOT, FUT)                         |
| 1020    | ✅ Valid     | None                    | Complete valid record (425.75, COMEX, OPT)                        |

### Expected Validation Results

After loading the sample data, you should see:

- **Total Records**: 21
- **Valid Records**: 11 (1001, 1002, 1004-first occurrence, 1006, 1007, 1008, 1009, 1010, 1015, 1019, 1020)
- **Invalid Records**: 10 (including 1 duplicate)
- **Duplicate Records**: 1 (GUID 1004 appears twice - first at index 3 is valid, second at index 4 is duplicate)
- **Missing Price**: 1 (1003)
- **Invalid Price Format**: 2 (1005, 1018)
- **Invalid Exchange**: 1 (1011)
- **Invalid Product Type**: 1 (1012)
- **Missing Exchange**: 1 (empty GUID record at index 15)
- **Missing Product Type**: 1 (1014)
- **Missing Trade Date**: 1 (1017)
- **Missing GUID**: 2 (empty GUID at index 15 and index 18)

---

## CLI Testing

### Starting CLI Mode

```bash
# Build the project
mvn clean package

# Run in CLI mode
java -jar target/pricing-validation-1.0.0.jar --cli
```

### CLI Menu Options

After starting the CLI, you'll see an interactive menu:

```
=== Pricing Data Validation & Reporting Utility ===

1. Load and validate CSV file
2. View validation report
3. View specific record
4. Update record
5. Delete record
6. Generate text report file
7. Exit

Enter your choice:
```

### Step-by-Step CLI Testing

#### Step 1: Load Data

1. Select option **1** from the menu
2. Enter file path: `sample_data/pricing_data.csv`
3. The system will:

   - Parse the CSV file
   - Validate all records
   - Display a summary:
     ```
     Data loaded successfully!
     Total Records: 21
     Valid Records: 11
     Invalid Records: 10
     Duplicate Records: 1
     Missing Records: 6
     ```

#### Step 2: View Validation Report

1. Select option **2** from the menu
2. The system displays:
   - Summary statistics
   - List of invalid records with error messages
   - Duplicate records list
   - Missing values breakdown
   - Complete record table with validation status

#### Step 3: View Specific Record

1. Select option **3** from the menu
2. Enter GUID: `1001` (or any GUID)
3. For duplicate GUIDs (e.g., `1004`):
   - System shows all matching records with indices
   - Select by entering the index number
4. For empty GUIDs:
   - Enter empty string (just press Enter)
   - System shows all records with empty GUIDs
   - Select by entering the index number

**Example Output:**

```
Record Details:
GUID: 1001
Trade Date: 2025-01-10
Price: 123.45
Exchange: CME
Product Type: FUT
Status: VALID
```

**Example - Viewing Duplicate Record:**

```
Enter Instrument GUID (or leave empty for records with no GUID): 1004

⚠ Multiple records found with GUID: 1004 (duplicates detected)
Please select by index from the full records list:
  [Index 3] Date: 2025-01-10, Price: 350.00, Status: VALID
  [Index 4] Date: 2025-01-10, Price: 350.00, Status: INVALID
Enter index: 4

--- Record Details ---
Instrument GUID: 1004
Trade Date: 2025-01-10
Price: 350.00
Exchange: CBOT
Product Type: FUT
Status: INVALID
Error: Duplicate GUID (primary key violation)
```

#### Step 4: Update Record

1. Select option **4** from the menu
2. Enter GUID: `1003` (missing price record)
3. Enter new price: `150.00`
4. The system updates the record and re-validates it
5. Record becomes **VALID** after correction

**Testing Duplicate Updates:**

- For GUID `1004` (duplicate):
  - System shows both records with indices
  - Enter index (e.g., `3` or `4`)
  - Update the selected record

**Example - Updating Empty GUID Record:**

```
Enter Instrument GUID (or leave empty for records with no GUID): [Press Enter]

⚠ Multiple records found with GUID: (empty) (duplicates detected)
Please select by index from the full records list:
  [Index 14] Date: 2025-01-10, Price: 200.00, Status: INVALID
  [Index 17] Date: 2025-01-10, Price: 250.00, Status: INVALID
Enter index: 14

Enter new price (or press Enter to skip): 200.00
Enter new exchange (or press Enter to skip): CME
Enter new product type (or press Enter to skip): FUT
Enter new trade date (YYYY-MM-DD) (or press Enter to skip): 2025-01-10

Record updated successfully!
```

#### Step 5: Delete Record

1. Select option **5** from the menu
2. Enter GUID: `1020` (valid record)
3. Confirm deletion
4. The system deletes the record and re-validates remaining records
5. If a duplicate was deleted, the remaining duplicate may become valid

**Testing Duplicate Deletion:**

**Example - Deleting Duplicate Record:**

```
Enter Instrument GUID (or leave empty for records with no GUID): 1004

⚠ Multiple records found with GUID: 1004 (duplicates detected)
Please select by index from the full records list:
  [Index 3] Date: 2025-01-10, Price: 350.00, Status: VALID
  [Index 4] Date: 2025-01-10, Price: 350.00, Status: INVALID
Enter index: 3

Are you sure you want to delete this record? (yes/no): yes

Record deleted successfully!
The remaining record at index 4 becomes VALID
```

**Note:** When you delete a duplicate record:

- If you delete the **first occurrence** (index 3), the second occurrence (index 4) will become **VALID** after re-validation (since it's now the only record with that GUID, making it unique and satisfying the primary key constraint)
- If you delete the **second occurrence** (index 4), the first occurrence (index 3) remains **VALID** (it was already valid as the first occurrence)
- After deletion, all records are automatically re-validated, which may change the validation status of remaining duplicates
- The validation report will update to reflect the new duplicate count (should be 0 after deleting one duplicate)

#### Step 6: Generate Text Report

1. Select option **6** from the menu
2. Enter output path: `validation_report.txt` (or press Enter for default)
3. The system generates a formatted text report file
4. Report saved to the specified location

#### Step 7: Exit

1. Select option **7** to exit the application

---

## API Testing

For API testing instructions, see **`API_DOCUMENTATION.md`** which includes:

- All API endpoints
- Request/response formats
- Postman testing guide

---


## Troubleshooting

### Common Issues

1. **"No data loaded" error:**

   - Solution: Load CSV file first using option 1 (CLI) or POST /load (API)

2. **"Record not found" error:**

   - Solution: Check GUID spelling, or use index parameter for duplicates

3. **"Multiple records found" error:**

   - Solution: Use `?index=` parameter to specify which duplicate record

4. **Empty GUID access:**
   - CLI: Enter empty string, then select by index
   - API: Use `EMPTY` as GUID placeholder with `?index=` parameter

---

For API-specific testing(postman), see **`API_DOCUMENTATION.md`**.
