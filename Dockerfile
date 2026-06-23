# --- Build stage ---
FROM eclipse-temurin:17-jdk-alpine AS build

RUN apk add --no-cache maven

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -Pdocker -DskipTests -B

# --- Runtime stage ---
FROM gcr.io/distroless/java17-debian12

COPY --from=build /app/target/http-proxy.jar /app/http-proxy.jar

ENV PORT=8080
EXPOSE 8080

USER nonroot:nonroot
ENTRYPOINT ["java", "-jar", "/app/http-proxy.jar"]
