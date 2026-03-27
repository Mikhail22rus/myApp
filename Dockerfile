FROM maven:3.8-openjdk-17 as builder
WORKDIR /app
COPY . .
RUN mvn clean package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# Убираем жёсткие значения — они будут переданы через переменные окружения в Render
# ENV строки УДАЛЯЕМ!

EXPOSE $PORT
CMD ["java", "-jar", "app.jar"]
