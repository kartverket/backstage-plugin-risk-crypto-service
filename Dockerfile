FROM eclipse-temurin:23.0.2_7-jdk-alpine-3.21 as build
COPY . .
RUN ./gradlew build -x test

FROM eclipse-temurin:23.0.2_7-jre-alpine-3.21

RUN mkdir -p /app /app/logs /app/tmp

ARG SOPS_AMD64="https://github.com/bekk/sops/releases/download/v3/sops-v3.linux.amd64"
ARG SOPS_ARM64="https://github.com/bekk/sops/releases/download/v3/sops-v3.linux.arm64"

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
RUN useradd user && chown -R user:user /app /app/logs /app/tmp

# Switch to non-root user.
USER user

EXPOSE 8080 8081
ENTRYPOINT ["java", "-jar", "/app/backend.jar"]