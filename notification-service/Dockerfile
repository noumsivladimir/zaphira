FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Copy the JAR file
COPY target/notification-service-1.0.0-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8007

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD java -cp app.jar org.springframework.boot.loader.JarLauncher || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "app.jar"]