## Etapa 1: Construcción con Maven y JDK 17
#FROM maven:3.8.8-eclipse-temurin-17 AS build
#
#WORKDIR /app
#
## Copia el código fuente al contenedor
#COPY . .
#
## Construye el proyecto con Maven
#RUN mvn clean install
#
## Etapa 2: Imagen ligera para ejecución
#FROM eclipse-temurin:17-jre-slim
#
#WORKDIR /app
#
## Copia el JAR generado desde la etapa de construcción
#COPY --from=build /app/modules/query-bridge/target/query-bridge-1.0.0.jar /app/query-bridge.jar
#
## Expone el puerto en el que correrá la aplicación
#EXPOSE 8081
#
## Comando para ejecutar la aplicación
#CMD ["java", "-jar", "/app/query-bridge.jar"]
