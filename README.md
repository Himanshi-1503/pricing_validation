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

**CLI Mode :**

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

> **📄 For validation rules and testing instructions, see [`TESTING_GUIDE.md`](TESTING_GUIDE.md)**  
> **📄 For API documentation, see [`API_DOCUMENTATION.md`](API_DOCUMENTATION.md)**

## Troubleshooting

**Port 8080 in use**: Change port in `src/main/resources/application.properties`:

```properties
server.port=8081
```

**Build errors**: Ensure Java 21 and Maven are properly installed:

```bash
java -version
mvn -version
```

**File not found**: Use paths relative to project root or absolute paths.

## Deployment

Deployed on Render: `https://pricing-validation-iab5.onrender.com`

## REST API

The application provides a REST API for programmatic access. See **`API_DOCUMENTATION.md`** for complete API reference.

**Deployed API:** `https://pricing-validation-iab5.onrender.com/api/pricing`

**Local Development:**

```bash
mvn clean package
java -jar target/pricing-validation-1.0.0.jar
```

Runs on `http://localhost:8080` by default.

## License

This project was developed as part of a pre-internship assignment at CME Group for learning and educational purposes related to pricing data validation and reporting.
