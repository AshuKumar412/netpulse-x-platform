# Stage 1: Build application using Maven and OpenJDK 25
FROM maven:3.9.9-eclipse-temurin-25-alpine AS builder

WORKDIR /build

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build package
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime environment
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# Create non-root system user for security
RUN addgroup -S netpulse && adduser -S netpulse -G netpulse

# Copy compiled JAR from builder
COPY --from=builder /build/target/*.jar app.jar

# Set ownership
RUN chown -R netpulse:netpulse /app
USER netpulse

EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
