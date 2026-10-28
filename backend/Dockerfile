# ---- Dev stage (Maven + JDK, the target docker compose uses) ----
FROM maven:3.9-eclipse-temurin-25 AS dev
WORKDIR /app
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -B


# ---- Build stage ----
FROM dev AS build
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn package -Dmaven.test.skip=true -B


# ---- Extract layers ----
# Splits the jar into layers (dependencies, loader, snapshot dependencies, application) for Docker caching.
FROM eclipse-temurin:25-jdk-jammy AS extract
WORKDIR /extract
COPY --from=build /app/target/*.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted


# ---- Runtime stage ----
FROM eclipse-temurin:25-jre-jammy
WORKDIR /app

RUN groupadd -r app && useradd -r -g app app

# One COPY per layer: Docker keeps the first three cached and only re-copies the application layer on code changes
COPY --from=extract /extract/extracted/dependencies/ ./
COPY --from=extract /extract/extracted/spring-boot-loader/ ./
COPY --from=extract /extract/extracted/snapshot-dependencies/ ./
COPY --from=extract /extract/extracted/application/ ./

# Run as a non-root user
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "application.jar"]