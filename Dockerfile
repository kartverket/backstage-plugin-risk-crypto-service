ARG BUILD_IMAGE=eclipse-temurin:23.0.2_7-jdk-alpine-3.21
ARG IMAGE=eclipse-temurin:23.0.2_7-jre-alpine-3.21
ARG SOPS_BUILD_IMAGE=golang:1.24.4

# Set SOPS_TAG=main to get latest
ARG SOPS_VERSION_ARG=3.10.2
# Set SOPS_TAG=main to get latest codebase
ARG SOPS_TAG=v${SOPS_VERSION_ARG}

### Build app ###
FROM ${BUILD_IMAGE} AS build

# Get security updates
RUN apk upgrade --no-cache

COPY . .
RUN ./gradlew build -x test -x smokeTest --no-daemon -Dorg.gradle.jvmargs="-Xmx1024m"

### Build Sops ###
FROM ${SOPS_BUILD_IMAGE} as sops_build

# Repeat ARG to use it
ARG SOPS_TAG
# Convert ARG to ENV to use in cmd
ENV SOPS_BRANCH=${SOPS_TAG}

# Set working directory inside the container
WORKDIR /build

# Install dependencies
RUN apt-get update && apt-get install -y \
    make \
    git \
 && rm -rf /var/lib/apt/lists/*

# Clone the sops repository for selected branch/tag. Turn off warning for detached head, and skip history
RUN git config --global advice.detachedHead false && \
    git clone --depth 1 --branch ${SOPS_BRANCH} https://github.com/getsops/sops.git

# Change working directory to sops source
WORKDIR /build/sops

# Build for selected arcitecure using make
ARG TARGETARCH
RUN if [ "$TARGETARCH" = "amd64" ]; then \
      GOOS=linux GOARCH=amd64 make install; \
      mv /go/bin/linux_amd64/sops /go/bin/sops; \
    elif [ "$TARGETARCH" = "arm64" ]; then \
      GOOS=linux GOARCH=arm64 make install; \
    else \
      echo "Unsupported architecture"; \
      exit 1; \
    fi

### Assemble image ###
FROM ${IMAGE}

# Add non-root user and set permissions.
RUN mkdir /app /app/logs /app/tmp && \
    adduser -D user && chown -R user:user /app /app/logs /app/tmp

# Copy jars, remove *plain.jar, and rename runnable jar
COPY --from=build /build/libs/*.jar /app/
RUN rm /app/*-plain.jar && mv /app/*.jar /app/backend.jar

# Sops version is checked in actuator/health
ARG SOPS_VERSION_ARG
ENV SOPS_VERSION=${SOPS_VERSION_ARG}

COPY --from=sops_build /go/bin/sops /usr/bin/

RUN chmod +x /usr/bin/sops
# Switch to non-root user.
USER user

EXPOSE 8080 8081
ENTRYPOINT ["java", "-jar", "/app/backend.jar"]

# Use the health endpoint of the application to provide information through docker about the health state of the application
HEALTHCHECK --start-period=30s --interval=5m \
   CMD wget -O - --quiet --tries=1 http://localhost:8083/actuator/health | grep UP || exit 1
