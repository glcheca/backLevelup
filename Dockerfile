# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

COPY gradle gradle
COPY gradlew gradlew.bat settings.gradle build.gradle ./
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon --quiet || true

COPY config config
COPY src src
RUN ./gradlew bootJar --no-daemon -x test \
    && mkdir -p /out \
    && find build/libs -maxdepth 1 -name '*.jar' ! -name '*-plain.jar' -exec cp {} /out/app.jar \;

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN groupadd --system levelup && useradd --system --gid levelup levelup
COPY --from=build /out/app.jar /app/app.jar

USER levelup
EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
