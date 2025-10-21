# ======================
# Step 1: Build with Maven
# ======================
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

# ======================
# Step 2: Run with JRE
# ======================
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy jar from build stage
COPY --from=build /app/target/*.jar app.jar

# ✅ Copy Firebase key (make sure file exists locally)
COPY src/main/resources/firebase-service-account.json /app/firebase-service-account.json

# Add non-root user
RUN useradd -m spring
USER spring

# Environment variables
ENV PORT=8080
ENV JAVA_OPTS="-Dserver.port=${PORT} -Dserver.address=0.0.0.0"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
