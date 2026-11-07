# syntax=docker/dockerfile:1
# Same image, built with the Maven root as the build context.
#   docker build -f strata/Dockerfile -t strata-minimal strata
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build
COPY . .
RUN mvn -B -pl examples/strata-minimal-app -am -DskipTests package \
    && cp examples/strata-minimal-app/target/strata-minimal-app-*.jar /build/app.jar

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /build/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
