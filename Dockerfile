# Etapa 1: Compilação do projeto
FROM maven:3.9-eclipse-temurin-21 AS build
COPY src /home/app/src
COPY pom.xml /home/app
RUN mvn -f /home/app/pom.xml clean package -DskipTests

# Etapa 2: Preparação do ambiente de execução
FROM eclipse-temurin:21-jre
COPY --from=build /home/app/target/sistema-escola-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]