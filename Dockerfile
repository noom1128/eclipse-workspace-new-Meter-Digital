# Stage 1: Build with Maven
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Production JRE Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN apk add --no-cache curl
COPY --from=build /app/target/meter-digital-0.0.1-SNAPSHOT.jar app.jar

# Default environment variables for Render
ENV PORT=8080
ENV SSL_ENABLED=false
EXPOSE 8080

# Launch with memory constrained for Render Free Tier (512 MB RAM)
ENTRYPOINT ["sh", "-c", "java -Xmx350m -Xss512k -Dserver.port=${PORT:-8080} -Dserver.ssl.enabled=false -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
