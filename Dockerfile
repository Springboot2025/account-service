# Step 1: Build with Maven
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Make mvnw executable
RUN chmod +x mvnw

# Download dependencies offline
RUN ./mvnw dependency:go-offline -B

# Copy source code and build jar
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Step 2: Run with JRE slim
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy jar from build stage as root
COPY --from=build /app/target/*.jar app.jar

# Make jar executable (still root)
RUN chmod +x app.jar

# Add non-root user
RUN useradd -m spring

# Switch to non-root user
USER spring

# No need for EXPOSE; Cloud Run uses $PORT automatically

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
