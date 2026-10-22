# ---- Dev stage (Built with pre-installed Maven) ----
FROM maven:3.9-eclipse-temurin-25 AS dev
WORKDIR /app
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -B


# ---- Build stage ----
FROM dev AS build
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn package -Dmaven.test.skip=true -B


# ---- Extract Layers ----
# unzips the JAR into specific layers for optimal Docker caching
FROM eclipse-temurin:25-jdk-jammy AS extract
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract


# ---- Runtime stage ----
FROM eclipse-temurin:25-jre-jammy
WORKDIR /app

RUN groupadd -r app && useradd -r -g app app

# Copy each layer individually.
# Docker will cache the first layers and only re-copy the 'application' layer on code changes.
COPY --from=extract /app/dependencies/ ./
COPY --from=extract /app/snapshot-dependencies/ ./
COPY --from=extract /app/application/ ./

# Secure container by running as non-root user
USER app
EXPOSE 8080

# Use JarLauncher instead of 'java -jar' to boot from the extracted layers natively
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]