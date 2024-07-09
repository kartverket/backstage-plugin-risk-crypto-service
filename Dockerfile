FROM eclipse-temurin:21.0.2_13-jre-alpine as build
COPY . .
RUN ./gradlew build -x test

FROM eclipse-temurin:21

RUN mkdir -p /app /app/logs /app/tmp
COPY --from=build /build/libs/*.jar /app/backend.jar

EXPOSE 8080 8081
ENTRYPOINT ["java", "-jar", "/app/backend.jar"]