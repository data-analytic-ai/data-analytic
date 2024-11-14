# Etapa 1: Construcción con Maven y JDK 17
FROM maven:3.9.9-amazoncorretto-17 AS build

WORKDIR /app

# Copia el código fuente al contenedor
COPY . .

# Construye el proyecto con Maven, ignorando las pruebas
RUN mvn clean install -DskipTests

# Etapa 2: Imagen ligera para ejecución
FROM amazoncorretto:17

WORKDIR /app

# Copia los JARs generados desde la etapa de construcción
COPY --from=build /app/modules/query-bridge/target/query-bridge-1.0.0.jar /app/query-bridge.jar
COPY --from=build /app/modules/data-bridge/target/data-bridge-1.0.0.jar /app/data-bridge.jar
COPY --from=build /app/modules/shared-library/target/shared-library-1.0.0.jar /app/shared-library.jar

# Expone el puerto en el que correrán las aplicaciones
EXPOSE 8081
EXPOSE 8082

# Comando para ejecutar el JAR principal (modificar según el JAR principal)
CMD ["java", "-jar", "/app/query-bridge.jar"]
