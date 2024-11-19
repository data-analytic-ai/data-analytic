# Etapa 1: Construcción con Maven y JDK 17
FROM maven:3.9.9-amazoncorretto-17 AS build

WORKDIR /app

# Copia el código fuente al contenedor
COPY . .

# Construye el proyecto con Maven (asegúrate de que genere un único .jar en target/)
RUN mvn clean package -DskipTests

# Etapa 2: Imagen ligera para ejecución
FROM amazoncorretto:17

WORKDIR /app

# Se Copia el JAR generado desde la etapa de construcción
COPY --from=build /app/target/data-analytic-1.0.0.jar /app/data-analytic.jar

# Expone el puerto en el que correrá la aplicación
EXPOSE 8081

# Comando para ejecutar la aplicación
#CMD ["java", "-jar", "/app/data-analytic.jar"]

# Set the entrypoint to allow passing JVM options and environment variables
ENTRYPOINT ["java", "-jar", "/app/data-analytic.jar"]
