FROM maven:3.9.11-eclipse-temurin-21-alpine AS build
WORKDIR /workspace
COPY pom.xml ./
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S kbase && adduser -S -G kbase kbase
WORKDIR /app
COPY --from=build /workspace/target/kbase-backend-*.jar app.jar
USER kbase
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
