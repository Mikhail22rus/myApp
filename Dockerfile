FROM openjdk:17-jdk-slim

WORKDIR /app

# Копируем собранный JAR файл
COPY build/libs/myApp-*.jar app.jar

# Открываем порт
EXPOSE 8080

# Запускаем приложение
ENTRYPOINT ["java", "-jar", "app.jar"]