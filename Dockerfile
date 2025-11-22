# Use Maven with OpenJDK 21
FROM maven:3.9-eclipse-temurin-21 AS build

# Set working directory
WORKDIR /app

# Copy pom.xml first (for better Docker layer caching)
COPY pom.xml .

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Final stage - use smaller JRE image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /app/target/pricing-validation-1.0.0.jar app.jar

# Expose port (Render will set PORT env variable)
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

