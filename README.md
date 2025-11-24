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

- **Java 21** (required - see `pom.xml`)
- **Maven 3.6 or higher**

**Installation (New System):**

1. **Install Java 21:**

   - Download from [Oracle](https://www.oracle.com/java/technologies/downloads/#java21) or [OpenJDK](https://adoptium.net/)
   - Verify: `java -version` (should show version 21.x.x)

2. **Install Maven:**
   - Download from [Apache Maven](https://maven.apache.org/download.cgi)
   - Add to PATH
   - Verify: `mvn -version` (should show version 3.6 or higher)

### Build and Run

**CLI Mode (Recommended):**

```bash
# Build the project
mvn clean package

# Run in CLI mode
java -jar target/pricing-validation-1.0.0.jar --cli
```

The CLI provides an interactive menu-driven interface for all operations.

**Note:** On a new system, Maven will automatically download all dependencies during the first `mvn clean package` command. This may take a few minutes.

## Usage

### CLI Mode (Interactive Menu)

After building and starting the application, you'll see an interactive menu:

**Menu Options:**

1. Load and validate CSV file
2. View validation report
3. View specific record
4. Update record
5. Delete record
6. Generate text report file
7. Exit

**Example Workflow:**

1. Start CLI: `java -jar target/pricing-validation-1.0.0.jar --cli`
2. Select option 1: Load CSV file (e.g., `sample_data/pricing_data.csv`)
3. Select option 2: View validation report
4. Select option 3: View specific record by GUID
5. Select option 4: Update records to fix errors
6. Select option 5: Delete records (if needed)
7. Select option 6: Generate text report file
8. Select option 7: Exit

## Validation Rules

The application enforces comprehensive validation rules to ensure data quality. Each record is validated for:

- Price validation (required, numeric, > 0)
- Exchange validation (CME, NYMEX, CBOT, COMEX)
- Product type validation (FUT, OPT)
- Instrument GUID validation (required, unique primary key)
- Trade date validation (YYYY-MM-DD format)
- Duplicate detection (GUID uniqueness)

> **📄 For complete validation rules and testing instructions, see [`TESTING_GUIDE.md`](TESTING_GUIDE.md)**

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

## Architecture & Logic

### System Architecture

The application follows a **layered architecture** pattern with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────┐
│                    REST API / CLI                        │
│              (Entry Points - User Interface)             │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│                  Controller Layer                         │
│            (PricingController.java)                       │
│  • Handles HTTP requests                                  │
│  • Request/Response mapping                               │
│  • Error handling                                         │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│                  Service Layer                            │
│            (PricingService.java)                          │
│  • Business logic orchestration                           │
│  • Data management                                        │
│  • Coordinates parser, validator, and report generator    │
└──────────┬───────────────────────────┬──────────────────┘
           │                           │
┌──────────▼──────────┐    ┌──────────▼──────────┐
│   Parser Layer       │    │  Validator Layer   │
│  (CSVParser.java)    │    │(PricingValidator)  │
│  • CSV file reading  │    │  • Rule validation│
│  • Data extraction    │    │  • Duplicate check│
│  • Type conversion    │    │  • Error tracking │
└──────────────────────┘    └───────────────────┘
           │                           │
           └──────────┬────────────────┘
                      │
         ┌────────────▼────────────┐
         │     Model Layer          │
         │  • PricingRecord          │
         │  • ValidationReport      │
         └────────────┬─────────────┘
                      │
         ┌────────────▼────────────┐
         │   Report Generator      │
         │  (ReportGenerator.java)  │
         │  • Text report creation  │
         │  • JSON report format    │
         └─────────────────────────┘
```

### Data Flow

1. **Input**: CSV file containing pricing data
2. **Parsing**: `CSVParser` reads CSV and converts rows to `PricingRecord` objects
3. **Validation**: `PricingValidator` applies all validation rules to each record
4. **Duplicate Detection**: Validator identifies duplicate GUIDs (primary key violations)
5. **Report Generation**: `ReportGenerator` creates comprehensive validation reports
6. **Storage**: Records and reports are stored in memory (service layer)
7. **Output**:
   - REST API returns JSON responses
   - CLI displays interactive menus and reports
   - Text files can be generated for reports

### Component Responsibilities

- **PricingController**: Handles HTTP requests, maps requests to service methods, formats responses
- **PricingService**: Orchestrates business logic, manages data state, coordinates components
- **CSVParser**: Reads CSV files, parses data, handles file I/O, converts strings to appropriate types
- **PricingValidator**: Applies validation rules, identifies duplicates, tracks errors
- **ReportGenerator**: Formats validation results into readable reports (JSON and text)
- **PricingRecord**: Data model representing a single pricing record
- **ValidationReport**: Data model containing validation summary and detailed results

## Project Structure

```
src/main/java/com/cme/pricing/
├── PricingValidationApplication.java  # Main application entry point
├── controller/
│   └── PricingController.java         # REST API endpoints (GET, POST, PUT, DELETE)
├── service/
│   └── PricingService.java            # Business logic and data management
├── validator/
│   └── PricingValidator.java          # Validation rules implementation
├── parser/
│   └── CSVParser.java                 # CSV file parsing and data extraction
├── model/
│   ├── PricingRecord.java             # Data model for pricing records
│   └── ValidationReport.java          # Data model for validation reports
├── report/
│   └── ReportGenerator.java          # Report generation (JSON and text)
└── cli/
    └── PricingCLI.java                # Command-line interface implementation
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

### Deploy to Render

**Render** offers free hosting for web services using Docker.

### Prerequisites

- ✅ GitHub repository with your code
- ✅ Dockerfile in repository root (already included)
- ✅ `application.properties` configured for PORT (already done)

### Step-by-Step Deployment

**Step 1: Sign Up**

1. Go to [render.com](https://render.com)
2. Click **"Get Started for Free"**
3. Sign up with your GitHub account (recommended) or email

**Step 2: Create New Web Service**

1. After logging in, click **"New +"** button (top right)
2. Select **"Web Service"** from the dropdown

**Step 3: Connect Repository**

1. Click **"Connect account"** if not already connected to GitHub
2. Authorize Render to access your GitHub repositories
3. Search for your repository: `pricing_validation` (or your repo name)
4. Click **"Connect"** next to your repository

**Step 4: Configure Service Settings**

Fill in the following configuration:

- **Name**: `pricing-validation` (or any name you prefer)
- **Region**: Select **"Singapore (Asia Pacific)** - closest to India for better latency
- **Branch**: `main` (or your default branch)
- **Root Directory**: Leave **empty** (default is root)
- **Runtime**: Select **"Docker"** (Java/Maven not available in Render)
- **Build Command**: Leave **empty** (Dockerfile handles this automatically)
- **Start Command**: Leave **empty** (Dockerfile handles this automatically)
- **Port**: Leave **empty** (Render automatically sets PORT environment variable)

**Step 5: Verify Configuration Files**

✅ **Dockerfile** (already in repository):

- Uses Maven to build the application
- Copies `sample_data` folder
- Creates production JAR
- Exposes port 8080
- Runs the application

✅ **application.properties** (already configured):

```properties
server.port=${PORT:8080}
```

This allows Render to set the port dynamically.

**Step 6: Deploy**

1. Click **"Create Web Service"** button
2. Render will:
   - Clone your repository
   - Build Docker image using Dockerfile
   - Start the container
   - Assign a public URL

**Step 7: Monitor Deployment**

1. Watch the **"Logs"** tab for build progress
2. First deployment takes **3-5 minutes** (building Docker image)
3. Look for: `Started PricingValidationApplication` in logs
4. Status changes to **"Live"** when ready

**Step 8: Get Your URL**

1. Once deployed, Render provides a URL like:
   ```
   `https://pricing-validation-iab5.onrender.com`
   ```
2. Copy this URL - this is your API base URL
3. Test it: `https://pricing-validation-iab5.onrender.com`

**Troubleshooting:**

- Check **Logs** tab in Render dashboard for errors
- Ensure Dockerfile is in repository root
- Verify `application.properties` has `server.port=${PORT:8080}`
- First request may timeout - wait 30 seconds and retry

## (Optional) Web Interface (REST API)

The application also provides a REST API web interface for programmatic access.

### Testing with Postman

To test the API endpoints, you'll need Postman:

1. **Download Postman:**

   - Visit [https://www.postman.com/downloads/](https://www.postman.com/downloads/)
   - Download the appropriate version for your operating system (Windows, macOS, or Linux)
   - Install Postman following the installation wizard

2. **Deployed API URL:**
   - Base URL: `https://pricing-validation-iab5.onrender.com/api/pricing`
   - All endpoints are available at this URL

**Start API Server (Local Development):**

```bash
# Build first
mvn clean package

# Run as REST API (default, no --cli flag)
java -jar target/pricing-validation-1.0.0.jar
```

The API runs on `http://localhost:8080` by default (for local development).

For complete API documentation and testing examples, see **`API_DOCUMENTATION.md`**.

## Documentation

- **`README.md`** (this file) - Project overview, architecture, quick start, deployment
- **`API_DOCUMENTATION.md`** - Complete API reference with endpoints, request/response formats, and Postman testing
- **`TESTING_GUIDE.md`** - Validation rules, sample dataset explanation, CLI testing steps, and complete testing workflow

## License

This project was developed as part of a pre-internship assignment at CME Group for learning and educational purposes related to pricing data validation and reporting.
