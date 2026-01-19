# Stage 1: Build the application - app.jar
FROM maven:3.8.8-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Extract the layers from the JAR built in the 'builder' stage
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract --destination .

# Stage 3: Copy dependencies as first step to leverage Docker cache
COPY --from=runtime dependencies/ ./dependencies/
COPY --from=runtime spring-boot-loader/ ./spring-boot-loader/
COPY --from=runtime snapshot-dependencies/ ./snapshot-dependencies/
COPY --from=runtime application/ ./application/

# Stage 4:Define the entrypoint to run the application using the layered structure
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]
EXPOSE 8080
