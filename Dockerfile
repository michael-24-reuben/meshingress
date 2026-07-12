# syntax=docker/dockerfile:1

FROM amazoncorretto:25 AS build

WORKDIR /workspace/meshingress

RUN dnf install -y tar gzip \
    && dnf clean all

COPY . .

RUN sed -i 's/\r$//' mvnw \
    && chmod +x mvnw \
    && ./mvnw --batch-mode --no-transfer-progress clean package -DskipTests

FROM amazoncorretto:25-alpine

WORKDIR /app

RUN addgroup -S meshingress \
    && adduser -S -G meshingress -h /app meshingress \
    && mkdir -p .cache/meshingress repository runtime tools/lib \
    && chown -R meshingress:meshingress /app

COPY --from=build \
    /workspace/meshingress/app/meshingress-server/target/*.jar \
    /app/meshingress.jar

RUN chown meshingress:meshingress /app/meshingress.jar

ENV SERVER_ADDRESS=0.0.0.0 \
    SERVER_PORT=8080

EXPOSE 8080

USER meshingress

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://127.0.0.1:${SERVER_PORT}/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/meshingress.jar"]
