FROM maven:3.8.4-openjdk-11 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src

RUN mvn clean package

FROM openjdk:11-jre-slim

WORKDIR /app
COPY --from=builder /app/target/websockets-spring-boot-0.0.1-SNAPSHOT.jar /app/app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
