# Etapa 1: build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /home/app

# Copie primeiro o pom para aproveitar cache
COPY pcomp/pom.xml ./pom.xml

# Copie o código do modulo-backend
COPY pcomp/modulo-backend ./modulo-backend

# Build (ajuste o -pl se for multi-módulo e necessário)
RUN mvn clean package -DskipTests

# Etapa 2: runtime
FROM eclipse-temurin:21-jre
WORKDIR /

COPY --from=build /home/app/target/sistema-generico-0.0.1.jar /app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]