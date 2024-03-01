FROM maven:3.6.3-openjdk-11 AS build
WORKDIR /app

COPY src /app/src
COPY pom.xml /app

RUN mvn clean package
FROM openjdk:11

WORKDIR /app

COPY --from=build /app/target/statistic-service-1.0.0.jar /app/statistic-service.jar

CMD ["java", "-jar", "statistic-service.jar"]
