# Testing on Render - Postman Guide

## 🚀 Quick Start for Render Deployment

### Step 1: Get Your Render URL

1. Go to [render.com](https://render.com) and log in
2. Navigate to your **Dashboard**
3. Click on your **Web Service** (pricing-validation or your app name)
4. Copy the **URL** - it will look like:
   ```
   https://your-app-name.onrender.com
   ```
   or
   ```
   https://pricing-validation-xxxx.onrender.com
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
     - **Initial Value**: `https://your-app-name.onrender.com`
     - **Current Value**: `https://your-app-name.onrender.com`
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
   - Replace: `https://your-app-name.onrender.com`
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
- **GET** `https://your-app-name.onrender.com/api/pricing/`
- **Expected**: Status 200, API information

### ✅ Step 2: Load Data
- **POST** `https://your-app-name.onrender.com/api/pricing/load`
- **Body** (raw → JSON):
  ```json
  {
    "filePath": "sample_data/pricing_data.csv"
  }
  ```
- **Expected**: Status 200, "Data loaded and validated successfully"

### ✅ Step 3: Get Report
- **GET** `https://your-app-name.onrender.com/api/pricing/report`
- **Expected**: Status 200, validation report with summary

### ✅ Step 4: Get All Records
- **GET** `https://your-app-name.onrender.com/api/pricing/records`
- **Expected**: Status 200, array of all records with indices

### ✅ Step 5: Get Specific Record
- **GET** `https://your-app-name.onrender.com/api/pricing/records/1001`
- **Expected**: Status 200, record details with index

### ✅ Step 6: Update Record
- **PUT** `https://your-app-name.onrender.com/api/pricing/records/1001`
- **Body** (raw → JSON):
  ```json
  {
    "price": 150.0
  }
  ```
- **Expected**: Status 200, updated record with index

### ✅ Step 7: Generate Report File
- **POST** `https://your-app-name.onrender.com/api/pricing/report/generate`
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
**URL**: `https://your-app-name.onrender.com/api/pricing/load`

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

Replace `your-app-name` with your actual Render app name:

```
https://your-app-name.onrender.com
```

**Example**:
```
https://pricing-validation-abc123.onrender.com
```

---

**Ready to test?** Start with Step 1 (Health Check) and work through the checklist! 🚀

