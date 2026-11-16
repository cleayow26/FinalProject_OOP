FROM eclipse-temurin:17-jdk AS build
WORKDIR /build
COPY gradlew gradlew.bat ./
COPY gradle/wrapper gradle/wrapper
COPY build.gradle settings.gradle ./
COPY src src
RUN chmod +x gradlew && ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre-jammy
RUN groupadd --system neonpulse && useradd --system --gid neonpulse --create-home neonpulse
WORKDIR /app
COPY --from=build /build/build/libs/*.jar app.jar
RUN chown -R neonpulse:neonpulse /app
USER neonpulse
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
