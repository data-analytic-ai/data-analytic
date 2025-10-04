# syntax=docker/dockerfile:1

# ---- build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -B -DskipTests dependency:go-offline
COPY src src
# build AND produce a single artifact for next stage
RUN mvn -q -B -DskipTests package && cp target/*jar app.jar

# ---- runtime ----
FROM eclipse-temurin:21-jre
WORKDIR /app
ENV SERVER_PORT=8081
ENV JAVA_OPTS=""
COPY --from=build /workspace/app.jar /app/app.jar
EXPOSE 8081
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar --server.port=${SERVER_PORT}"]
