FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /home/app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /

COPY --from=build /home/app/target/*.jar /app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]