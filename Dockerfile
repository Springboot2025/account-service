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

# Step 2: Run with JRE slim, non-root user
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Add non-root user
RUN useradd -m spring
USER spring

# Copy jar from build stage
COPY --from=build /app/target/*.jar app.jar
RUN chmod +x app.jar

# No need for EXPOSE — Cloud Run maps PORT automatically

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
