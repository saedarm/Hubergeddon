# Hubergeddon dedicated server (no graphics). Used by `fly deploy`.
FROM eclipse-temurin:17-jdk AS build
WORKDIR /src
COPY . .
RUN chmod +x gradlew && ./gradlew --no-daemon -q core:serverJar

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /src/core/build/libs/Hubergeddon-server.jar /app/server.jar
EXPOSE 7777
CMD ["java", "-Xmx160m", "-XX:+UseSerialGC", "-jar", "/app/server.jar"]
