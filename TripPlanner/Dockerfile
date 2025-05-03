# syntax=docker/dockerfile:1.6

# -------------------- STAGE 1: build --------------------
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /src

# 1) Копируем wrapper + gradle-скрипты (для кэша)
COPY gradlew .
COPY gradle gradle
COPY settings.gradle.kts build.gradle.kts ./
RUN chmod +x gradlew

# 2) Прогреваем кеш зависимостей
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon dependencies || true

# 3) Копируем весь исходный код и ресурсы, собираем shadowJar
COPY src src
COPY src/main/resources src/main/resources
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon shadowJar -x test

# -------------------- STAGE 2: runtime --------------------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Копируем единственный jar из build/libs → /app/app.jar
COPY --from=builder /src/build/libs/*.jar app.jar

# Пробрасываем порт
EXPOSE 8080

# По умолчанию просто запускаем jar
ENTRYPOINT ["java", "-jar", "app.jar"]