# ============================================================
# Stage 1: Build
# ============================================================
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

# Copy Maven wrapper and pom first for layer caching
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies (cached unless pom.xml changes)
RUN ./mvnw dependency:go-offline -B

# Copy source and build
COPY src src
RUN ./mvnw package -DskipTests -B

# ============================================================
# Stage 2: Runtime
# ============================================================
FROM eclipse-temurin:21-jre-jammy

# Metadata
LABEL org.opencontainers.image.title="resume-match-api" \
      org.opencontainers.image.description="Spring Boot REST API for Resume Matcher" \
      org.opencontainers.image.source="https://github.com/debageriab-prog/resume-match-api"

ENV JAVA_OPTS="-Xms256m -Xmx512m"

# Create non-root user
RUN useradd -m -u 1001 appuser

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

RUN chown appuser:appuser app.jar

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=10s --start-period=40s --retries=5 \
    CMD curl -f http://localhost:8080/api/v1/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
