# Pricing Data Validation & Reporting Utility

A Java Spring Boot application for validating pricing data from CSV files. Validates records, identifies errors, and provides both REST API and CLI interfaces for data management.

## Features

- CSV file parsing and validation
- Identifies missing values, duplicates, and invalid formats
- Generates detailed validation reports
- REST API for programmatic access
- Interactive CLI for local operations
- Handles duplicate records with index-based selection

## Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher

### Build and Run

**Development mode** (recommended for testing):

```bash
# Run as REST API (default)
mvn spring-boot:run

# Run in CLI mode
mvn spring-boot:run -Dspring-boot.run.arguments=--cli
```

**Production mode** (build JAR first):

```bash
# Build the project
mvn clean package

# Run as REST API (default)
java -jar target/pricing-validation-1.0.0.jar

# Run in CLI mode
java -jar target/pricing-validation-1.0.0.jar --cli
```

The API runs on `http://localhost:8080` by default.

## Usage

### CLI Mode (Interactive Menu)

Run the application in CLI mode for an interactive menu-driven interface:

```bash
# Development
mvn spring-boot:run -Dspring-boot.run.arguments=--cli

# Production
java -jar target/pricing-validation-1.0.0.jar --cli
```

**Menu Options:**

1. Load and validate CSV file
2. View validation report
3. View specific record
4. Update record
5. Delete record
6. Generate text report file
7. Exit

### REST API

The application runs as a REST API by default, providing HTTP endpoints for all operations.

**Start the API server:**

```bash
mvn spring-boot:run
# or
java -jar target/pricing-validation-1.0.0.jar
```

The API will be available at `http://localhost:8080`

**Quick Start Examples:**

**1. Load data:**

```bash
curl -X POST http://localhost:8080/api/pricing/load \
  -H "Content-Type: application/json" \
  -d '{"filePath":"sample_data/pricing_data.csv"}'
```

**2. Get validation report:**

```bash
curl http://localhost:8080/api/pricing/report
```

**3. Update a record:**

```bash
curl -X PUT http://localhost:8080/api/pricing/records/1003 \
  -H "Content-Type: application/json" \
  -d '{"price":150.00}'
```

For PowerShell commands and more examples, see `API_COMMANDS_QUICK_REFERENCE.md`.

## Validation Rules

Records are validated against:

- **Price**: Must be present, numeric, and greater than zero
- **Exchange**: Must be one of: CME, NYMEX, CBOT, COMEX
- **Product Type**: Must be FUT or OPT
- **Instrument GUID**: Required, non-empty
- **Trade Date**: Required, format: YYYY-MM-DD
- **Duplicates**: Detects exact duplicate records (all fields match)

## Handling Duplicates

When multiple records share the same GUID, the API returns a 409 Conflict with a list of all matching records. Use the `index` parameter to target a specific record:

```bash
# Get list of duplicates
curl http://localhost:8080/api/pricing/records/1004

# Access specific record by index
curl http://localhost:8080/api/pricing/records/1004?index=3
```

## API Endpoints

| Method | Endpoint                              | Description                |
| ------ | ------------------------------------- | -------------------------- |
| POST   | `/api/pricing/load`                   | Load and validate CSV file |
| GET    | `/api/pricing/report`                 | Get validation report      |
| POST   | `/api/pricing/report/generate`        | Generate text report file  |
| GET    | `/api/pricing/records`                | Get all records            |
| GET    | `/api/pricing/records/{guid}`         | Get specific record        |
| PUT    | `/api/pricing/records/{guid}`         | Update record              |
| DELETE | `/api/pricing/records/{guid}`         | Delete record              |
| POST   | `/api/pricing/records/{guid}/correct` | Correct invalid record     |

See `API_DOCUMENTATION.md` for detailed request/response formats.

## Project Structure

```
src/main/java/com/cme/pricing/
├── PricingValidationApplication.java  # Main application
├── controller/
│   └── PricingController.java         # REST endpoints
├── service/
│   └── PricingService.java            # Business logic
├── validator/
│   └── PricingValidator.java          # Validation rules
├── parser/
│   └── CSVParser.java                 # CSV parsing
├── model/
│   ├── PricingRecord.java             # Data model
│   └── ValidationReport.java          # Report model
└── report/
    └── ReportGenerator.java           # Report generation
```

## Sample Data

The project includes `sample_data/pricing_data.csv` with various validation scenarios:

- Missing values (price, exchange, GUID, date)
- Invalid formats (non-numeric prices, invalid exchange/product types)
- Duplicate records

## Output

The validation report includes:

- Summary statistics (total, valid, invalid, duplicates)
- Error breakdown by type
- Detailed lists of invalid records
- Duplicate records with full details
- Missing values categorized by field
- Complete table of all records with status

## Troubleshooting

**Port 8080 in use**: Change port in `src/main/resources/application.properties`:

```properties
server.port=8081
```

**Build errors**: Ensure Java 17+ and Maven are properly installed:

```bash
java -version
mvn -version
```

**File not found**: Use paths relative to project root or absolute paths.

## Deployment

### Step 1: Build the Application

First, build the production JAR file:

```bash
mvn clean package
```

This creates `target/pricing-validation-1.0.0.jar` - a standalone executable JAR file.

### Step 2: Choose Deployment Platform

#### Option A: Railway (Recommended - Free & Easy)

**Railway** is the easiest platform for deploying Spring Boot apps.

1. **Sign up**: Go to [railway.app](https://railway.app) and sign up with GitHub
2. **Create new project**: Click "New Project" → "Deploy from GitHub repo"
3. **Select your repository**: Choose your project repository
4. **Configure build**:
   - Build Command: `mvn clean package -DskipTests`
   - Start Command: `java -jar target/pricing-validation-1.0.0.jar`
   - Port: Railway automatically sets `PORT` environment variable
5. **Update application.properties** (if needed):
   ```properties
   server.port=${PORT:8080}
   ```
6. **Deploy**: Railway will automatically build and deploy your app
7. **Get URL**: Railway provides a URL like `https://your-app.railway.app`

**Note**: Railway's free tier includes 500 hours/month and $5 credit.

---

#### Option B: Render (Free Tier Available)

**Render** offers free hosting for web services.

1. **Sign up**: Go to [render.com](https://render.com) and sign up
2. **Create new Web Service**: Click "New" → "Web Service"
3. **Connect repository**: Link your GitHub repository
4. **Configure** (using Docker since Java is not available):

   - **Name**: `pricing-validation` (or any name)
   - **Language**: Select **`Docker`** (Java/Maven not available in Render)
   - **Branch**: `main`
   - **Region**: Choose **Singapore (Asia Pacific)** - closest to India
   - **Root Directory**: Leave empty
   - **Build Command**: Leave empty (Docker will use Dockerfile automatically)
   - **Start Command**: Leave empty (Docker will use Dockerfile automatically)
   - **Port**: Leave empty (Render automatically sets PORT environment variable)

   ✅ **Dockerfile is already in your repository** - Render will automatically detect and use it!

5. **Verify application.properties** (already configured):
   ```properties
   server.port=${PORT:8080}
   ```
   ✅ Your current `application.properties` is already correct! Render will automatically set the `PORT` environment variable, and your app will use it.
6. **Deploy**: Click "Create Web Service"
7. **Get URL**: Render provides a URL like `https://your-app.onrender.com`

**Note**: Free tier apps spin down after 15 minutes of inactivity (takes ~30 seconds to wake up).

---

#### Option C: Traditional Server (VPS/Cloud Server)

For deploying on your own server (AWS EC2, DigitalOcean, etc.):

1. **Upload JAR file** to your server:

   ```bash
   scp target/pricing-validation-1.0.0.jar user@your-server:/path/to/app/
   ```

2. **Install Java 21** on the server:

   ```bash
   sudo apt update
   sudo apt install openjdk-21-jdk
   ```

3. **Run the application**:

   ```bash
   java -jar pricing-validation-1.0.0.jar
   ```

4. **Run as a service** (optional - for auto-restart):

   Create `/etc/systemd/system/pricing-api.service`:

   ```ini
   [Unit]
   Description=Pricing Validation API
   After=network.target

   [Service]
   Type=simple
   User=your-user
   WorkingDirectory=/path/to/app
   ExecStart=/usr/bin/java -jar /path/to/app/pricing-validation-1.0.0.jar
   Restart=always
   RestartSec=10

   [Install]
   WantedBy=multi-user.target
   ```

   Then:

   ```bash
   sudo systemctl daemon-reload
   sudo systemctl enable pricing-api
   sudo systemctl start pricing-api
   ```

5. **Configure firewall** (if needed):
   ```bash
   sudo ufw allow 8080/tcp
   ```

---

### Step 3: Update API Base URL

After deployment, update your API calls to use the deployed URL:

**Local:**

```bash
http://localhost:8080/api/pricing/load
```

**Deployed (Railway example):**

```bash
https://your-app.railway.app/api/pricing/load
```

### Step 4: Test Deployment

Test your deployed API:

```bash
# Get API info
curl https://your-app.railway.app/api/pricing/

# Load data (note: file paths need to be accessible on server)
curl -X POST https://your-app.railway.app/api/pricing/load \
  -H "Content-Type: application/json" \
  -d '{"filePath":"sample_data/pricing_data.csv"}'
```

### Important Notes for Deployment

1. **File Paths**: When deployed, file paths are relative to the application's working directory. You may need to upload CSV files to the server or use absolute paths.

2. **Port Configuration**: Most cloud platforms set the `PORT` environment variable. Update `application.properties`:

   ```properties
   server.port=${PORT:8080}
   ```

3. **Environment Variables**: You can configure the app using environment variables:

   ```bash
   export SERVER_PORT=8080
   export LOGGING_LEVEL=INFO
   ```

4. **Logs**: Check platform logs for errors:
   - Railway: Dashboard → Your App → Logs
   - Render: Dashboard → Your Service → Logs

## Documentation

- `API_DOCUMENTATION.md` - Complete API reference
- `API_COMMANDS_QUICK_REFERENCE.md` - PowerShell/curl commands
- `TEST_GET_ENDPOINTS_BROWSER.md` - Browser testing guide

## License

This project was developed as a learning exercise for pricing data validation.
