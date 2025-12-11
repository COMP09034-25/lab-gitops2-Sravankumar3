# Stage 1: Build the application
FROM gradle:8.5-jdk21 AS builder
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY src ./src
RUN gradle bootJar --no-daemon

# Stage 2: Create the runtime image
FROM eclipse-temurin:21-jre
RUN useradd spring
USER spring
WORKDIR /workspace
# COPY --from=builder /workspace/catalog-service/dependencies ./
# COPY --from=builder /workspace/catalog-service/spring-boot-loader ./
# COPY --from=builder /workspace/catalog-service/snapshot-dependencies ./
# COPY --from=builder /workspace/catalog-service/application ./
# ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
COPY --from=builder /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
