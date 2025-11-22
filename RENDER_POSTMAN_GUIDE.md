# Testing on Render - Postman Guide

## 🚀 Quick Start for Render Deployment

### Step 1: Get Your Render URL

1. Go to [render.com](https://render.com) and log in
2. Navigate to your **Dashboard**
3. Click on your **Web Service** (pricing-validation or your app name)
4. Copy the **URL**:
   ```
   https://pricing-validation-iab5.onrender.com
   ```

### Step 2: Update Postman Requests

**Replace `http://localhost:8080` with your Render URL in all requests.**

#### Option A: Use Postman Variables (Recommended)

1. **Create Environment Variable:**

   - Click **Environments** in Postman (left sidebar)
   - Click **+** to create new environment
   - Name: `Render Production`
   - Add variable:
     - **Variable**: `base_url`
     - **Initial Value**: `https://pricing-validation-iab5.onrender.com`
     - **Current Value**: `https://pricing-validation-iab5.onrender.com`
   - Click **Save**

2. **Update All Requests:**

   - In each request URL, replace:
     ```
     http://localhost:8080
     ```
     with:
     ```
     {{base_url}}
     ```
   - Example:
     - Old: `http://localhost:8080/api/pricing/`
     - New: `{{base_url}}/api/pricing/`

3. **Select Environment:**
   - Top-right corner of Postman
   - Select **"Render Production"** from dropdown

#### Option B: Find and Replace (Quick Method)

1. In Postman, use **Find and Replace**:
   - Press `Ctrl+H` (Windows) or `Cmd+H` (Mac)
   - Find: `http://localhost:8080`
   - Replace: `https://pricing-validation-iab5.onrender.com`
   - Click **Replace All**

---

## ⚠️ Important Notes for Render

### 1. **First Request May Be Slow**

- Render free tier apps **spin down after 15 minutes of inactivity**
- First request after spin-down takes **~30 seconds** to wake up
- Subsequent requests are fast (normal speed)

### 2. **Test Endpoints in Order**

Follow the same order as the localhost guide:

1. **GET** `/api/pricing/` - Check if API is running
2. **POST** `/api/pricing/load` - Load data (use `sample_data/pricing_data.csv` path)
3. **GET** `/api/pricing/report` - View report
4. **GET** `/api/pricing/records` - Get all records
5. Continue with other endpoints...

### 3. **File Path for Load Endpoint**

When loading data on Render, use the path relative to the Docker container:

```json
{
  "filePath": "sample_data/pricing_data.csv"
}
```

The Dockerfile already copies `sample_data` folder, so this path works.

---

## 📋 Quick Test Checklist

Test these endpoints in order:

### ✅ Step 1: Health Check

- **GET** `https://pricing-validation-iab5.onrender.com/api/pricing/`
- **Expected**: Status 200, API information

### ✅ Step 2: Load Data

- **POST** `https://pricing-validation-iab5.onrender.com/api/pricing/load`
- **Body** (raw → JSON):
  ```json
  {
    "filePath": "sample_data/pricing_data.csv"
  }
  ```
- **Expected**: Status 200, "Data loaded and validated successfully"

### ✅ Step 3: Get Report

- **GET** `https://pricing-validation-iab5.onrender.com/api/pricing/report`
- **Expected**: Status 200, validation report with summary

### ✅ Step 4: Get All Records

- **GET** `https://pricing-validation-iab5.onrender.com/api/pricing/records`
- **Expected**: Status 200, array of all records with indices

### ✅ Step 5: Get Specific Record

- **GET** `https://pricing-validation-iab5.onrender.com/api/pricing/records/1001`
- **Expected**: Status 200, record details with index

### ✅ Step 6: Update Record

- **PUT** `https://pricing-validation-iab5.onrender.com/api/pricing/records/1001`
- **Body** (raw → JSON):
  ```json
  {
    "price": 150.0
  }
  ```
- **Expected**: Status 200, updated record with index

### ✅ Step 7: Generate Report File

- **POST** `https://pricing-validation-iab5.onrender.com/api/pricing/report/generate`
- **Body** (raw → JSON):
  ```json
  {}
  ```
- **Expected**: Status 200, report generated message

---

## 🔍 Troubleshooting

### Issue: "Connection timeout" or "Request timeout"

- **Solution**: Render app is spinning up. Wait 30 seconds and try again.

### Issue: "404 Not Found"

- **Solution**: Check your Render URL is correct. Make sure it includes `https://` not `http://`

### Issue: "500 Internal Server Error"

- **Solution**: Check Render logs:
  1. Go to Render Dashboard
  2. Click your service
  3. Click **Logs** tab
  4. Check for error messages

### Issue: "File not found" when loading data

- **Solution**: Make sure Dockerfile copied `sample_data` folder (it should be already done)

---

## 🎯 Example: Complete Request Setup

### Request: Load Data on Render

**Method**: POST  
**URL**: `https://pricing-validation-iab5.onrender.com/api/pricing/load`

**Headers**:

```
Content-Type: application/json
```

**Body** (raw → JSON):

```json
{
  "filePath": "sample_data/pricing_data.csv"
}
```

**Expected Response**:

```json
{
  "message": "Data loaded and validated successfully!",
  "summary": {
    "totalRecords": 21,
    "validRecords": 11,
    "invalidRecords": 10,
    "duplicateRecords": 1,
    "missingValues": 5
  }
}
```

---

## 💡 Pro Tips

1. **Save All Requests**: Create a Postman Collection with all requests saved
2. **Use Environment Variables**: Switch between localhost and Render easily
3. **Check Logs**: Render logs show all API requests and errors
4. **Test After Deployment**: Always test immediately after deploying to catch issues early

---

## 📝 Your Render URL

Your Render deployment URL:

```
https://pricing-validation-iab5.onrender.com
```

**Base API URL**:

```
https://pricing-validation-iab5.onrender.com/api/pricing
```

---

---

## 🔥 Complete API Reference - All Endpoints & Edge Cases

### Base URL
```
https://pricing-validation-iab5.onrender.com/api/pricing
```

---

## 📋 All API Endpoints

### 1. **GET** `/api/pricing/` - API Information
- **URL**: `https://pricing-validation-iab5.onrender.com/api/pricing/`
- **Method**: GET
- **Body**: None
- **Expected**: Status 200, API info

---

### 2. **POST** `/api/pricing/load` - Load CSV Data
- **URL**: `https://pricing-validation-iab5.onrender.com/api/pricing/load`
- **Method**: POST
- **Body** (JSON):
  ```json
  {
    "filePath": "sample_data/pricing_data.csv"
  }
  ```
- **Expected**: Status 200, data loaded successfully

---

### 3. **GET** `/api/pricing/report` - Get Validation Report
- **URL**: `https://pricing-validation-iab5.onrender.com/api/pricing/report`
- **Method**: GET
- **Body**: None
- **Expected**: Status 200, validation report with summary

---

### 4. **POST** `/api/pricing/report/generate` - Generate Report File
- **URL**: `https://pricing-validation-iab5.onrender.com/api/pricing/report/generate`
- **Method**: POST
- **Body** (JSON - **required**, even if empty):
  ```json
  {}
  ```
- **Expected**: Status 200, report generated message

---

### 5. **GET** `/api/pricing/records` - Get All Records
- **URL**: `https://pricing-validation-iab5.onrender.com/api/pricing/records`
- **Method**: GET
- **Body**: None
- **Expected**: Status 200, array of all records with indices

---

### 6. **GET** `/api/pricing/records/{guid}` - Get Specific Record
- **URL**: `https://pricing-validation-iab5.onrender.com/api/pricing/records/1001`
- **Method**: GET
- **Body**: None
- **Expected**: Status 200, record details with index

**Edge Case - Duplicate GUID:**
- **URL**: `https://pricing-validation-iab5.onrender.com/api/pricing/records/1004`
- **Expected**: Status 409 Conflict, list of duplicate records with indices
- **Solution**: Use `?index` parameter: `GET /api/pricing/records/1004?index=3`

**Edge Case - Null/Empty GUID:**
- **URL**: `https://pricing-validation-iab5.onrender.com/api/pricing/records/EMPTY?index=17`
- **Method**: GET
- **Expected**: Status 200, record at index 17 (null GUID)

---

### 7. **PUT** `/api/pricing/records/{guid}` - Update Record
- **URL**: `https://pricing-validation-iab5.onrender.com/api/pricing/records/1001`
- **Method**: PUT
- **Body** (JSON - all fields optional):
  ```json
  {
    "price": 150.0,
    "exchange": "CME",
    "productType": "FUT",
    "tradeDate": "2025-01-10"
  }
  ```
- **Expected**: Status 200, updated record with index

**Edge Case - Update with Index (for duplicates):**
- **URL**: `https://pricing-validation-iab5.onrender.com/api/pricing/records/1004?index=3`
- **Method**: PUT
- **Body** (JSON):
  ```json
  {
    "price": 200.0
  }
  ```
- **Expected**: Status 200, updated record at index 3

**Edge Case - Update Null GUID Record:**
- **URL**: `https://pricing-validation-iab5.onrender.com/api/pricing/records/EMPTY?index=17`
- **Method**: PUT
- **Body** (JSON):
  ```json
  {
    "price": 250.0,
    "exchange": "COMEX"
  }
  ```
- **Expected**: Status 200, updated record

**Edge Case - Duplicate GUID (Index Required):**
- **URL**: `https://pricing-validation-iab5.onrender.com/api/pricing/records/1004`
- **Method**: PUT
- **Expected**: Status 400 Bad Request, "Index parameter is required"
- **Solution**: Add `?index=3` to URL

---

### 8. **POST** `/api/pricing/records/{guid}/correct` - Correct Invalid Record
- **URL**: `https://pricing-validation-iab5.onrender.com/api/pricing/records/1003/correct`
- **Method**: POST
- **Body** (JSON - can include GUID correction):
  ```json
  {
    "price": 150.0,
    "exchange": "CME"
  }
  ```
- **Expected**: Status 200, corrected record with index

**Edge Case - Correct Missing GUID:**
- **URL**: `https://pricing-validation-iab5.onrender.com/api/pricing/records/EMPTY/correct?index=17`
- **Method**: POST
- **Body** (JSON):
  ```json
  {
    "instrumentGuid": "1023",
    "price": 250.0,
    "exchange": "COMEX",
    "productType": "OPT"
  }
  ```
- **Expected**: Status 200, record corrected with new GUID

**Edge Case - Correct Duplicate GUID:**
- **URL**: `https://pricing-validation-iab5.onrender.com/api/pricing/records/1004/correct?index=4`
- **Method**: POST
- **Body** (JSON):
  ```json
  {
    "instrumentGuid": "1024",
    "price": 200.0
  }
  ```
- **Expected**: Status 200, duplicate GUID changed to unique GUID

**Edge Case - Duplicate GUID (Index Required):**
- **URL**: `https://pricing-validation-iab5.onrender.com/api/pricing/records/1004/correct`
- **Method**: POST
- **Expected**: Status 400 Bad Request, "Index parameter is required"
- **Solution**: Add `?index=4` to URL

**Edge Case - Correct Invalid Price Format:**
- **URL**: `https://pricing-validation-iab5.onrender.com/api/pricing/records/1005/correct`
- **Method**: POST
- **Body** (JSON):
  ```json
  {
    "price": 175.0
  }
  ```
- **Expected**: Status 200, price corrected

**Edge Case - Correct Invalid Exchange:**
- **URL**: `https://pricing-validation-iab5.onrender.com/api/pricing/records/1008/correct`
- **Method**: POST
- **Body** (JSON):
  ```json
  {
    "exchange": "CME"
  }
  ```
- **Expected**: Status 200, exchange corrected

**Edge Case - Correct Invalid Product Type:**
- **URL**: `https://pricing-validation-iab5.onrender.com/api/pricing/records/1009/correct`
- **Method**: POST
- **Body** (JSON):
  ```json
  {
    "productType": "FUT"
  }
  ```
- **Expected**: Status 200, product type corrected

**Edge Case - Correct Negative Price:**
- **URL**: `https://pricing-validation-iab5.onrender.com/api/pricing/records/1011/correct`
- **Method**: POST
- **Body** (JSON):
  ```json
  {
    "price": 100.0
  }
  ```
- **Expected**: Status 200, price corrected

**Edge Case - Correct Zero Price:**
- **URL**: `https://pricing-validation-iab5.onrender.com/api/pricing/records/1012/correct`
- **Method**: POST
- **Body** (JSON):
  ```json
  {
    "price": 50.0
  }
  ```
- **Expected**: Status 200, price corrected

**Edge Case - Correct Missing Trade Date:**
- **URL**: `https://pricing-validation-iab5.onrender.com/api/pricing/records/1013/correct`
- **Method**: POST
- **Body** (JSON):
  ```json
  {
    "tradeDate": "2025-01-10"
  }
  ```
- **Expected**: Status 200, trade date added

---

### 9. **DELETE** `/api/pricing/records/{guid}` - Delete Record
- **URL**: `https://pricing-validation-iab5.onrender.com/api/pricing/records/1020`
- **Method**: DELETE
- **Body**: None
- **Expected**: Status 200, deletion confirmation with deletedIndex

**Edge Case - Delete with Index (for duplicates):**
- **URL**: `https://pricing-validation-iab5.onrender.com/api/pricing/records/1004?index=4`
- **Method**: DELETE
- **Expected**: Status 200, record at index 4 deleted

**Edge Case - Delete Null GUID Record:**
- **URL**: `https://pricing-validation-iab5.onrender.com/api/pricing/records/EMPTY?index=17`
- **Method**: DELETE
- **Expected**: Status 200, record deleted

**Edge Case - Duplicate GUID (Index Required):**
- **URL**: `https://pricing-validation-iab5.onrender.com/api/pricing/records/1004`
- **Method**: DELETE
- **Expected**: Status 400 Bad Request, "Index parameter is required"
- **Solution**: Add `?index=3` or `?index=4` to URL

---

## 🚨 Error Scenarios & Edge Cases

### Error 1: Duplicate GUID Without Index
**Scenario**: Multiple records with same GUID (e.g., 1004 appears twice)

**Request:**
```
GET https://pricing-validation-iab5.onrender.com/api/pricing/records/1004
```

**Response**: Status 409 Conflict
```json
{
  "message": "Multiple records found with GUID: 1004",
  "count": 2,
  "records": [
    {
      "index": 3,
      "instrumentGuid": "1004",
      ...
    },
    {
      "index": 4,
      "instrumentGuid": "1004",
      ...
    }
  ],
  "instruction": "Use GET /api/pricing/records/1004?index={index} to view a specific record"
}
```

**Solution**: Use `?index` parameter
```
GET https://pricing-validation-iab5.onrender.com/api/pricing/records/1004?index=3
```

---

### Error 2: Null/Empty GUID Access
**Scenario**: Record has null or empty GUID

**Step 1: Find Null GUID Records**
```
GET https://pricing-validation-iab5.onrender.com/api/pricing/records
```
- Look for records with `instrumentGuid: null` or `instrumentGuid: ""`
- Note the index (e.g., index 17)

**Step 2: Access Null GUID Record**
```
GET https://pricing-validation-iab5.onrender.com/api/pricing/records/EMPTY?index=17
```

**Expected**: Status 200, record details

---

### Error 3: GUID Mismatch (Index Verification)
**Scenario**: Trying to update/delete record with wrong GUID in URL

**Request:**
```
PUT https://pricing-validation-iab5.onrender.com/api/pricing/records/1004?index=5
```
(If index 5 has GUID 1005, not 1004)

**Response**: Status 400 Bad Request
```json
{
  "error": "GUID mismatch: Record at index 5 has GUID '1005', but you specified '1004'. This prevents accidental update of wrong records."
}
```

**Solution**: Use correct GUID or correct index

---

### Error 4: Invalid Price Value
**Scenario**: Trying to update with price <= 0

**Request:**
```
PUT https://pricing-validation-iab5.onrender.com/api/pricing/records/1001
Body: { "price": -10.0 }
```

**Response**: Status 400 Bad Request
```json
{
  "error": "Price must be greater than zero"
}
```

---

### Error 5: Duplicate GUID When Correcting
**Scenario**: Trying to correct record with GUID that already exists

**Request:**
```
POST https://pricing-validation-iab5.onrender.com/api/pricing/records/1003/correct
Body: { "instrumentGuid": "1001" }
```
(If 1001 already exists)

**Response**: Status 400 Bad Request
```json
{
  "error": "Correction failed. The new GUID may already exist or price is invalid."
}
```

**Solution**: Use a unique GUID

---

### Error 6: Record Not Found
**Scenario**: GUID doesn't exist or was deleted

**Request:**
```
GET https://pricing-validation-iab5.onrender.com/api/pricing/records/9999
```

**Response**: Status 404 Not Found
```json
{
  "error": "Record not found for GUID: 9999"
}
```

---

## 📊 Complete Testing Workflow

### Phase 1: Setup & Load Data
1. ✅ GET `/api/pricing/` - Health check
2. ✅ POST `/api/pricing/load` - Load CSV data
3. ✅ GET `/api/pricing/report` - View initial report

### Phase 2: View Operations
4. ✅ GET `/api/pricing/records` - Get all records
5. ✅ GET `/api/pricing/records/1001` - Get valid record
6. ✅ GET `/api/pricing/records/1003` - Get invalid record
7. ✅ GET `/api/pricing/records/1004` - Handle duplicate (see list)
8. ✅ GET `/api/pricing/records/1004?index=3` - Get specific duplicate
9. ✅ GET `/api/pricing/records/EMPTY?index=17` - Get null GUID record

### Phase 3: Update Operations
10. ✅ PUT `/api/pricing/records/1003` - Update missing price
11. ✅ PUT `/api/pricing/records/1002` - Update multiple fields
12. ✅ PUT `/api/pricing/records/1004?index=3` - Update duplicate with index

### Phase 4: Correction Operations
13. ✅ POST `/api/pricing/records/1005/correct` - Fix invalid price format
14. ✅ POST `/api/pricing/records/1004/correct?index=4` - Fix duplicate GUID
15. ✅ POST `/api/pricing/records/EMPTY/correct?index=17` - Fix missing GUID
16. ✅ POST `/api/pricing/records/1008/correct` - Fix invalid exchange
17. ✅ POST `/api/pricing/records/1009/correct` - Fix invalid product type
18. ✅ POST `/api/pricing/records/1011/correct` - Fix negative price
19. ✅ POST `/api/pricing/records/1012/correct` - Fix zero price
20. ✅ POST `/api/pricing/records/1013/correct` - Fix missing trade date

### Phase 5: Report & Verification
21. ✅ GET `/api/pricing/report` - Verify improvements
22. ✅ POST `/api/pricing/report/generate` - Generate report file

### Phase 6: Delete Operations (Optional)
23. ✅ DELETE `/api/pricing/records/1020` - Delete record
24. ✅ DELETE `/api/pricing/records/1004?index=4` - Delete duplicate with index
25. ✅ DELETE `/api/pricing/records/EMPTY?index=17` - Delete null GUID record

---

## 🎯 Quick Reference: All GUIDs & Their Status

| GUID    | Status    | Issue                | Test Endpoint                                    |
| ------- | --------- | -------------------- | ----------------------------------------------- |
| 1001    | Valid     | None                 | `GET /api/pricing/records/1001`                 |
| 1002    | Valid     | None                 | `PUT /api/pricing/records/1002`                 |
| 1003    | Invalid   | Missing price        | `PUT /api/pricing/records/1003` or `/correct`  |
| 1004    | Duplicate | Appears twice        | `GET /api/pricing/records/1004?index=3`        |
| 1005    | Invalid   | Invalid price format | `POST /api/pricing/records/1005/correct`       |
| 1006    | Valid     | None                 | -                                               |
| 1007    | Invalid   | Missing exchange     | -                                               |
| 1008    | Invalid   | Invalid exchange     | `POST /api/pricing/records/1008/correct`       |
| 1009    | Invalid   | Missing product type | `POST /api/pricing/records/1009/correct`       |
| 1010    | Invalid   | Invalid product type | -                                               |
| 1011    | Invalid   | Negative price       | `POST /api/pricing/records/1011/correct`       |
| 1012    | Invalid   | Zero price           | `POST /api/pricing/records/1012/correct`       |
| 1013    | Invalid   | Missing trade date   | `POST /api/pricing/records/1013/correct`       |
| 1014    | Valid     | None                 | -                                               |
| 1015    | Valid     | None                 | -                                               |
| (empty) | Invalid   | Missing GUID         | `GET /api/pricing/records/EMPTY?index=17`      |
| 1017    | Invalid   | Missing trade date   | -                                               |
| 1018    | Invalid   | Invalid price format | -                                               |
| 1019    | Valid     | None                 | -                                               |
| 1020    | Valid     | None                 | `DELETE /api/pricing/records/1020`              |
| 1021    | Valid     | None                 | -                                               |

---

## 💡 Pro Tips for Edge Cases

1. **Always use `?index` for duplicates**: When GUID appears multiple times, index is required
2. **Use `EMPTY` placeholder for null GUIDs**: `GET /api/pricing/records/EMPTY?index=17`
3. **Check GUID before operations**: Use GET first to verify GUID matches index
4. **Unique GUIDs when correcting**: When fixing missing GUIDs, ensure new GUID is unique
5. **Verify after operations**: Always GET the record after UPDATE/CORRECT to verify changes

---

**Ready to test?** Start with Phase 1 and work through all phases! 🚀
