
# Etapa 1: Compilação do projeto
FROM maven:3.8.5-openjdk-17 AS build
LABEL authors="Evandro Sacramento"
COPY . .
RUN mvn clean package -DskipTests

# Etapa 2: Preparação do ambiente de execução
FROM openjdk:17.0.1-jdk-slim
COPY --from=build /target/nome-generico-do-seu-app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]