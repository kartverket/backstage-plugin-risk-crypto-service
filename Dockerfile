ARG BUILD_IMAGE=eclipse-temurin:24.0.1_9-jdk-alpine-3.21
ARG IMAGE=eclipse-temurin:24.0.1_9-jre-alpine-3.21

FROM ${BUILD_IMAGE} AS build

# Get security updates
RUN apk upgrade --no-cache

COPY . .
RUN ./gradlew build -x test -x smokeTest

FROM ${IMAGE}

# Get security updates
RUN apk upgrade --no-cache

RUN mkdir -p /app /app/logs /app/tmp

ARG SOPS_VERSION="v3.10.2"

ARG SOPS_AMD64="https://github.com/getsops/sops/releases/download/$SOPS_VERSION/sops-$SOPS_VERSION.linux.amd64"
ARG SOPS_ARM64="https://github.com/getsops/sops/releases/download/$SOPS_VERSION/sops-$SOPS_VERSION.linux.arm64"

ARG TARGETARCH
RUN if [ "$TARGETARCH" = "amd64" ]; then \
    wget $SOPS_AMD64 -O /usr/local/bin/sops; \
  elif [ "$TARGETARCH" = "arm64" ]; then \
    wget $SOPS_ARM64 -O /usr/local/bin/sops; \
  else \
    echo "Unsupported architecture"; \
    exit 1; \
  fi \
  && chmod +x /usr/local/bin/sops

COPY --from=build /build/libs/*.jar /app/backend.jar

# Add non-root user and change permissions.
RUN adduser -D user && chown -R user:user /app /app/logs /app/tmp

# Switch to non-root user.
USER user

EXPOSE 8080 8081
ENTRYPOINT ["java", "-jar", "/app/backend.jar"]
