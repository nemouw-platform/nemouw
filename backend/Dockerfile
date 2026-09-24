# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom first so dependency downloads are cached across rebuilds
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Now copy the source and build the fat jar
COPY src ./src
RUN mvn package -DskipTests


# ---- Runtime stage ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /app/target/nemouw-backend-1.0-SNAPSHOT.jar /app/backend.jar

EXPOSE 4567
ENTRYPOINT ["java", "-jar", "/app/backend.jar"]