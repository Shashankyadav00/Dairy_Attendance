# Use Java 17 as the base image
FROM eclipse-temurin:17

# Set the working directory
WORKDIR /app

# Copy all project files
COPY . .

# Give executable permission to mvnw (required on Linux)
RUN chmod +x mvnw

# Build the project using Maven Wrapper
RUN ./mvnw clean package -DskipTests

# Expose port 8080
EXPOSE 8080

# Run the built JAR
CMD ["java", "-jar", "target/backend-0.0.1-SNAPSHOT.jar"]
