# syntax=docker/dockerfile:1
# Single-image build of the minimal open-source application (examples/strata-minimal-app).
#   docker build -t strata-minimal .
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build
COPY strata/ strata/
WORKDIR /build/strata
RUN mvn -B -pl examples/strata-minimal-app -am -DskipTests package \
    && cp examples/strata-minimal-app/target/strata-minimal-app-*.jar /build/app.jar

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /build/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
