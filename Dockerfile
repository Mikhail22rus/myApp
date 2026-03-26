FROM maven:3.8-openjdk-17 as builder
WORKDIR /app
COPY . .
RUN mvn clean package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# Установка утилит для диагностики
RUN apt-get update && apt-get install -y dnsutils curl

# Диагностика DNS
RUN echo "=== Testing DNS resolution ===" && \
    nslookup eclwbktyxozelrclshlz.supabase.co && \
    ping -c 1 eclwbktyxozelrclshlz.supabase.co || echo "Ping failed"

EXPOSE $PORT
CMD ["sh", "-c", "java -Dserver.port=$PORT -jar app.jar"]
