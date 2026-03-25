# Etapa de build
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app


COPY quality-evaluator-api ./quality-evaluator-api


WORKDIR /app/quality-evaluator-api


RUN mvn clean package -DskipTests


FROM eclipse-temurin:21-jdk
WORKDIR /app


COPY --from=build /app/quality-evaluator-api/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]