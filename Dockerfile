FROM maven:3.8-openjdk-17 as builder
WORKDIR /app
COPY . .
RUN mvn clean package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# Диагностика: выводим переменные окружения
RUN echo "=== Environment variables ===" && env | grep SPRING

EXPOSE $PORT
CMD ["sh", "-c", "echo '=== SPRING_DATASOURCE_URL=' $SPRING_DATASOURCE_URL && java -Dserver.port=$PORT -jar app.jar"]
