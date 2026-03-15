# payment-switch-lab Dockerfile
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY target/payment-switch-lab.jar /app/app.jar
COPY src/dist/deploy /app/deploy
COPY src/main/resources/cfg /app/cfg

EXPOSE 8583

HEALTHCHECK --interval=30s --timeout=5s \
    CMD echo "health" | nc -w 1 localhost 8583 || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
