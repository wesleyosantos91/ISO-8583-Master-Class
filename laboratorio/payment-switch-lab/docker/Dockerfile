# payment-switch-lab Dockerfile
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# Copiar artefatos do build
COPY txn-manager/target/txn-manager-*.jar /app/app.jar
COPY src/dist/deploy /app/deploy
COPY src/main/resources/cfg /app/cfg

EXPOSE 8583
EXPOSE 9091

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
