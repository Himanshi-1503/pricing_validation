# Complete Postman Guide - Step by Step with Real GUIDs

## Your Data Contains These GUIDs:

- `1001` - Valid record
- `1002` - Valid record
- `1003` - Invalid (missing price)
- `1004` - Duplicate (appears twice at index 3 and 4)
- `1005` - Invalid (invalid price format "INVALID")
- `1006` - Valid record
- `1007` - Invalid (missing exchange - empty)
- `1008` - Invalid (invalid exchange "INVALID")
- `1009` - Invalid (missing product type - empty)
- `1010` - Invalid (invalid product type "INVALID")
- `1011` - Invalid (negative price -320.00)
- `1012` - Invalid (zero price 0)
- `1013` - Invalid (missing trade date - empty)
- `1014` - Valid record
- `1015` - Valid record
- `(empty)` - Invalid (missing GUID at index 17)
- `1017` - Invalid (missing trade date)
- `1018` - Invalid (invalid price format "ABC123")
- `1019` - Valid record
- `1020` - Valid record

---

## 🚀 Complete Testing Guide - Start to Finish

### Prerequisites

- ✅ Postman installed
- ✅ Application running locally: `http://localhost:8080`
- ✅ Collection created: "Pricing Validation API"

---

## STEP 1: Get API Information ✅

**Purpose:** Verify API is running

1. **Create Request:**

   - Click "Add Request" in your collection
   - Name: `1. Get API Info`

2. **Configure:**

   - Method: **GET**
   - URL: `http://localhost:8080/api/pricing/`
   - Body: None
   - Headers: None needed

3. **Send:**
   - Click **Send**
   - **Expected:** Status 200 OK with API information

---

## STEP 2: Load CSV Data ✅

**Purpose:** Load and validate your pricing data

1. **Create Request:**

   - Click "Add Request"
   - Name: `2. Load CSV Data`

2. **Configure:**

   - Method: **POST**
   - URL: `http://localhost:8080/api/pricing/load`
   - Go to **Body** tab
   - Select **raw**
   - Select **JSON** from dropdown
   - Enter:

   ```json
   {
     "filePath": "sample_data/pricing_data.csv"
   }
   ```

3. **Send:**
   - Click **Send**
   - **Expected:** Status 200 OK
   - Response should show: `totalRecords: 21`, `validRecords: 10`, `invalidRecords: 11`

**✅ IMPORTANT:** This must be done FIRST before testing other endpoints!

---

## STEP 3: Get Validation Report

**Purpose:** See complete validation summary

1. **Create Request:**

   - Name: `3. Get Validation Report`

2. **Configure:**

   - Method: **GET**
   - URL: `http://localhost:8080/api/pricing/report`
   - Body: None

3. **Send:**
   - Click **Send**
   - **Expected:** Status 200 OK
   - Shows all records with validation status

---

## STEP 4: Get All Records

**Purpose:** View all records sorted by GUID

1. **Create Request:**

   - Name: `4. Get All Records`

2. **Configure:**

   - Method: **GET**
   - URL: `http://localhost:8080/api/pricing/records`
   - Body: None

3. **Send:**
   - Click **Send**
   - **Expected:** Status 200 OK
   - Returns array of all 21 records

---

## STEP 5: Get Specific Valid Record

**Purpose:** View a valid record (GUID 1001)

1. **Create Request:**

   - Name: `5. Get Record 1001 (Valid)`

2. **Configure:**

   - Method: **GET**
   - URL: `http://localhost:8080/api/pricing/records/1001`
   - Body: None

3. **Send:**
   - Click **Send**
   - **Expected:** Status 200 OK
   - Shows record with `valid: true`

---

## STEP 6: Get Invalid Record (Missing Price)

**Purpose:** View an invalid record (GUID 1003 - missing price)

1. **Create Request:**

   - Name: `6. Get Record 1003 (Invalid - Missing Price)`

2. **Configure:**

   - Method: **GET**
   - URL: `http://localhost:8080/api/pricing/records/1003`
   - Body: None

3. **Send:**
   - Click **Send**
   - **Expected:** Status 200 OK
   - Shows record with `valid: false`, `validationError: "Missing price"`

---

## STEP 7: Handle Duplicate GUID

**Purpose:** See how duplicates are handled (GUID 1004 appears twice)

1. **Create Request:**

   - Name: `7. Get Record 1004 (Duplicate - Conflict)`

2. **Configure:**

   - Method: **GET**
   - URL: `http://localhost:8080/api/pricing/records/1004`
   - Body: None

3. **Send:**

   - Click **Send**
   - **Expected:** Status 409 Conflict
   - Response shows list of all records with GUID 1004

4. **Get Specific Duplicate:**
   - Create new request: `7b. Get Record 1004 Index 0`
   - Method: **GET**
   - URL: `http://localhost:8080/api/pricing/records/1004?index=0`
   - **Expected:** Status 200 OK with first occurrence

---

## STEP 7c: Get Record with Null/Empty GUID

**Purpose:** View records that have missing/null GUIDs (use index parameter)

1. **First, find the index of records with empty GUID:**

   - Use request from Step 4 (Get All Records)
   - Look for records where `instrumentGuid` is empty or null
   - Note the index (position in the array, starting from 0)
   - In your data, empty GUIDs are at index 16

2. **Create Request:**

   - Name: `7c. Get Record with Empty GUID (Index 16)`

3. **Configure:**

   - Method: **GET**
   - URL: `http://localhost:8080/api/pricing/records/EMPTY?index=16`
   - Body: None
   - **Note:** Use "EMPTY" as placeholder GUID, `?index=16` to specify which record

4. **Send:**
   - Click **Send**
   - **Expected:** Status 200 OK
   - Shows record with empty GUID at index 16

**Note:** In your data, there is only one record with empty GUID at index 16.

**If you don't know the index or have multiple null GUIDs:** See the section at the end of this guide: "How to Handle Null GUIDs When You Don't Know the Index"

---

## STEP 8: Update Invalid Record

**Purpose:** Fix the invalid record (GUID 1003) by adding a price

1. **Create Request:**

   - Name: `8. Update Record 1003 (Add Price)`

2. **Configure:**

   - Method: **PUT**
   - URL: `http://localhost:8080/api/pricing/records/1003`
   - Go to **Body** tab
   - Select **raw** → **JSON**
   - Enter:

   ```json
   {
     "price": 150.0
   }
   ```

3. **Send:**

   - Click **Send**
   - **Expected:** Status 200 OK
   - Message: "Record updated successfully"
   - Record should now be `valid: true`

4. **Verify Update:**
   - Use request from Step 6 again
   - Should now show `valid: true` and `price: 150.00`

---

## STEP 9: Update Multiple Fields

**Purpose:** Update multiple fields at once (GUID 1004)

1. **Create Request:**

   - Name: `9. Update Record 1004 (Multiple Fields)`

2. **Configure:**

   - Method: **PUT**
   - URL: `http://localhost:8080/api/pricing/records/1004`
   - Body (raw → JSON):

   ```json
   {
     "price": 250.0,
     "exchange": "NYMEX",
     "productType": "OPT"
   }
   ```

3. **Send:**
   - Click **Send**
   - **Expected:** Status 200 OK
   - All fields updated

---

## STEP 10: Correct Invalid Record with GUID Fix

**Purpose:** Fix invalid record and assign new GUID (GUID 1005 has invalid price)

1. **Create Request:**

   - Name: `10. Correct Record 1005 (Fix Price)`

2. **Configure:**

   - Method: **POST**
   - URL: `http://localhost:8080/api/pricing/records/1005/correct`
   - Body (raw → JSON):

   ```json
   {
     "price": 275.5
   }
   ```

3. **Send:**
   - Click **Send**
   - **Expected:** Status 200 OK
   - Shows `recordIsNowValid: true`

---

## STEP 11: Correct Duplicate GUID

**Purpose:** Fix duplicate by assigning new GUID to second occurrence

1. **Create Request:**

   - Name: `11. Correct Record 1004 Index 4 (New GUID)`

2. **Configure:**

   - Method: **POST**
   - URL: `http://localhost:8080/api/pricing/records/1004/correct?index=4`
   - Body (raw → JSON):

   ```json
   {
     "instrumentGuid": "1022",
     "price": 400.0
   }
   ```

3. **Send:**
   - Click **Send**
   - **Expected:** Status 200 OK
   - Second duplicate now has new GUID 1022

---

## STEP 12: Fix Missing GUID

**Purpose:** Assign GUID to record with missing GUID (Index 17)

1. **First, get the record with empty GUID:**

   - Use request from Step 7c to see the record at index 17
   - Or use Step 4 (Get All Records) to find empty GUID records
   - In your data, empty GUID is at index 16

2. **Create Request:**

   - Name: `12. Correct Missing GUID (Index 16)`

3. **Configure:**

   - Method: **POST**
   - URL: `http://localhost:8080/api/pricing/records/EMPTY/correct?index=16`
   - Body (raw → JSON):

   ```json
   {
     "instrumentGuid": "1023",
     "price": 250.0,
     "exchange": "COMEX",
     "productType": "OPT"
   }
   ```

4. **Send:**
   - Click **Send**
   - **Expected:** Status 200 OK
   - Record now has GUID 1023
   - Record should become valid if all other fields are correct

**If you don't know the index or have multiple null GUIDs:** See the section at the end of this guide: "How to Handle Null GUIDs When You Don't Know the Index"

---

## STEP 13: Fix Invalid Exchange

**Purpose:** Correct record with invalid exchange (GUID 1008)

1. **Create Request:**

   - Name: `13. Correct Record 1008 (Fix Exchange)`

2. **Configure:**

   - Method: **POST**
   - URL: `http://localhost:8080/api/pricing/records/1008/correct`
   - Body (raw → JSON):

   ```json
   {
     "exchange": "CME"
   }
   ```

3. **Send:**
   - Click **Send**
   - **Expected:** Status 200 OK

---

## STEP 14: Fix Invalid Product Type

**Purpose:** Correct record with invalid product type (GUID 1009 or 1010)

1. **Create Request:**

   - Name: `14. Correct Record 1009 (Fix Product Type)`

2. **Configure:**

   - Method: **POST**
   - URL: `http://localhost:8080/api/pricing/records/1009/correct`
   - Body (raw → JSON):

   ```json
   {
     "productType": "FUT"
   }
   ```

3. **Send:**
   - Click **Send**
   - **Expected:** Status 200 OK

---

## STEP 15: Fix Negative Price

**Purpose:** Correct record with negative price (GUID 1011)

1. **Create Request:**

   - Name: `15. Correct Record 1011 (Fix Negative Price)`

2. **Configure:**

   - Method: **POST**
   - URL: `http://localhost:8080/api/pricing/records/1011/correct`
   - Body (raw → JSON):

   ```json
   {
     "price": 100.0
   }
   ```

3. **Send:**
   - Click **Send**
   - **Expected:** Status 200 OK

---

## STEP 16: Fix Zero Price

**Purpose:** Correct record with zero price (GUID 1012)

1. **Create Request:**

   - Name: `16. Correct Record 1012 (Fix Zero Price)`

2. **Configure:**

   - Method: **POST**
   - URL: `http://localhost:8080/api/pricing/records/1012/correct`
   - Body (raw → JSON):

   ```json
   {
     "price": 50.0
   }
   ```

3. **Send:**
   - Click **Send**
   - **Expected:** Status 200 OK

---

## STEP 17: Fix Missing Trade Date

**Purpose:** Correct record with missing trade date (GUID 1013)

1. **Create Request:**

   - Name: `17. Correct Record 1013 (Add Trade Date)`

2. **Configure:**

   - Method: **POST**
   - URL: `http://localhost:8080/api/pricing/records/1013/correct`
   - Body (raw → JSON):

   ```json
   {
     "tradeDate": "2025-01-10"
   }
   ```

3. **Send:**
   - Click **Send**
   - **Expected:** Status 200 OK

---

## STEP 18: Generate Text Report

**Purpose:** Create a text file with the complete report

1. **Create Request:**

   - Name: `18. Generate Text Report`

2. **Configure:**

   - Method: **POST**
   - URL: `http://localhost:8080/api/pricing/report/generate`
   - Go to **Body** tab
   - Select **raw** → **JSON**
   - Enter:

   ```json
   {}
   ```

   - **OR** specify custom path:

   ```json
   {
     "outputPath": "validation_report.txt"
   }
   ```

   - **⚠️ Important:** This endpoint **requires a JSON body** (even if empty `{}`). Don't leave body empty!

3. **Send:**
   - Click **Send**
   - **Expected:** Status 200 OK
   - Message: "Report generated successfully"
   - Check your project folder for `validation_report.txt`

---

## STEP 19: Verify All Corrections

**Purpose:** Check that all records are now valid

1. **Use Request from Step 3:**

   - Get Validation Report again
   - **Expected:** More valid records, fewer invalid records

2. **Use Request from Step 4:**
   - Get All Records
   - Check that corrected records show `valid: true`

---

## STEP 20: Delete Record (Optional - Be Careful!)

**Purpose:** Test deletion functionality

1. **Create Request:**

   - Name: `20. Delete Record 1020`

2. **Configure:**

   - Method: **DELETE**
   - URL: `http://localhost:8080/api/pricing/records/1020`
   - Body: None

3. **Send:**

   - Click **Send**
   - **Expected:** Status 200 OK
   - Message: "Record deleted successfully"

4. **Verify Deletion:**
   - Try to get the record: `GET /api/pricing/records/1020`
   - **Expected:** Status 404 Not Found

---

## 📊 Complete Testing Checklist

### Basic Operations

- [ ] Step 1: Get API Info
- [ ] Step 2: Load CSV Data
- [ ] Step 3: Get Validation Report
- [ ] Step 4: Get All Records

### View Operations

- [ ] Step 5: Get Valid Record (1001)
- [ ] Step 6: Get Invalid Record (1003)
- [ ] Step 7: Handle Duplicate (1004)
- [ ] Step 7c Part A: Find All Null GUID Records
- [ ] Step 7c Part B: Get Specific Null GUID Record (when index known)

### Update Operations

- [ ] Step 8: Update Record (1003 - add price)
- [ ] Step 9: Update Multiple Fields (1002)

### Correction Operations

- [ ] Step 10: Correct Invalid Price (1005)
- [ ] Step 11: Correct Duplicate GUID (1004 index 1)
- [ ] Step 12: Fix Missing GUID (single or multiple)
- [ ] Step 13: Fix Invalid Exchange (1008)
- [ ] Step 14: Fix Invalid Product Type (1009)
- [ ] Step 15: Fix Negative Price (1011)
- [ ] Step 16: Fix Zero Price (1012)
- [ ] Step 17: Fix Missing Trade Date (1013)

### Report Operations

- [ ] Step 18: Generate Text Report

### Verification

- [ ] Step 19: Verify All Corrections

### Delete Operation (Optional)

- [ ] Step 20: Delete Record (1020)

---

## 🎯 Quick Reference: All GUIDs in Your Data

| GUID    | Status    | Issue                | Test Step              |
| ------- | --------- | -------------------- | ---------------------- |
| 1001    | Valid     | None                 | Step 5                 |
| 1002    | Valid     | None                 | Step 9                 |
| 1003    | Invalid   | Missing price        | Step 6, 8              |
| 1004    | Duplicate | Appears twice        | Step 7, 11             |
| 1005    | Invalid   | Invalid price format | Step 10                |
| 1006    | Valid     | None                 | -                      |
| 1007    | Invalid   | Missing exchange     | -                      |
| 1008    | Invalid   | Invalid exchange     | Step 13                |
| 1009    | Invalid   | Missing product type | Step 14                |
| 1010    | Invalid   | Invalid product type | -                      |
| 1011    | Invalid   | Negative price       | Step 15                |
| 1012    | Invalid   | Zero price           | Step 16                |
| 1013    | Invalid   | Missing trade date   | Step 17                |
| 1014    | Valid     | None                 | -                      |
| 1015    | Valid     | None                 | -                      |
| (empty) | Invalid   | Missing GUID         | Step 7c, 12 (index 17) |
| 1018    | Valid     | None                 | -                      |
| 1019    | Valid     | None                 | -                      |
| 1020    | Valid     | None                 | Step 20 (delete)       |
| 1021    | Valid     | None                 | -                      |

---

## 💡 Tips

1. **Save Responses:** After each request, save the response as an example
2. **Use Variables:** Create environment variables for base URL
3. **Test in Order:** Follow steps 1-20 in sequence
4. **Verify Changes:** After updates/corrections, get the record again to verify
5. **Check Reports:** After corrections, regenerate report to see improvements

---

## 🚨 Common Issues

### 404 Not Found

- Record doesn't exist or was deleted
- Check GUID is correct

### 409 Conflict

- Multiple records with same GUID
- Use `?index` parameter

### 400 Bad Request

- Invalid JSON format
- Missing required fields
- Invalid data types

### 500 Internal Server Error

- Server issue
- Check application logs

---

---

## 🔍 How to Handle Null GUIDs When You Don't Know the Index

### Problem: You have null/empty GUIDs but don't know their indices

### Solution: Find All Null GUIDs First

**Step 1: Get All Records**

- Method: **GET**
- URL: `http://localhost:8080/api/pricing/records`
- This returns an array of all records

**Step 2: Identify Null GUID Records**

- Look through the JSON array response
- Find records where `instrumentGuid` is:
  - `null`
  - `""` (empty string)
  - Missing the field
- **Count the position** in the array (starts from 0)
  - First record = index 0
  - Second record = index 1
  - Third record = index 2
  - etc.

**Example:**

```json
[
  0: { "instrumentGuid": "1001", ... },  // Index 0
  1: { "instrumentGuid": "1002", ... },  // Index 1
  2: { "instrumentGuid": null, ... },     // Index 2 ← NULL GUID!
  3: { "instrumentGuid": "1004", ... },   // Index 3
  4: { "instrumentGuid": "", ... }        // Index 4 ← EMPTY GUID!
]
```

**Step 3: Get Specific Null GUID Record**

- Once you know the index (e.g., 2), use:
- `GET /api/pricing/records/EMPTY?index=2`

### Handling Multiple Null GUIDs

**If you found 3 null GUIDs at indices 2, 4, and 7:**

1. **Get each one:**

   - `GET /api/pricing/records/EMPTY?index=2`
   - `GET /api/pricing/records/EMPTY?index=4`
   - `GET /api/pricing/records/EMPTY?index=7`

2. **Fix each one with unique GUIDs:**
   - `POST /api/pricing/records/EMPTY/correct?index=2` → `{"instrumentGuid": "1023", ...}`
   - `POST /api/pricing/records/EMPTY/correct?index=4` → `{"instrumentGuid": "1024", ...}`
   - `POST /api/pricing/records/EMPTY/correct?index=7` → `{"instrumentGuid": "1025", ...}`

**⚠️ Important:** Each GUID must be unique! Use 1023, 1024, 1025, etc.

---

Happy Testing! Follow these steps from 1-20 to test everything! 🎉
