# ===== Stage 1: Build the application =====
FROM eclipse-temurin:17 as builder
WORKDIR /app
COPY . .
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

# ===== Stage 2: Run the application =====
FROM eclipse-temurin:17
WORKDIR /app

# Copy only the built JAR from the previous stage
COPY --from=builder /app/target/backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
