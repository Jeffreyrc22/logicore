FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -ntp package
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S logicore && adduser -S logicore -G logicore
WORKDIR /app
COPY --from=build /app/target/logicore-api-1.0.0.jar app.jar
USER logicore
EXPOSE 8090
ENTRYPOINT ["java","-jar","app.jar"]
