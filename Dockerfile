# Use Java 17 runtime
FROM eclipse-temurin:17

# Set working directory
WORKDIR /app

# Copy project files
COPY . .

# Build the project using Maven Wrapper
RUN ./mvnw clean package

# Expose port 8080
EXPOSE 8080

# Run the built JAR file
CMD ["java", "-jar", "target/backend-0.0.1-SNAPSHOT.jar"]
