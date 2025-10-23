ARG JVM_BUILD_IMAGE=eclipse-temurin:24.0.2_12-jdk-alpine-3.22
ARG JRE_IMAGE=eclipse-temurin:24.0.2_12-jre-alpine-3.22
ARG SOPS_BUILD_IMAGE=golang:1.24.6

# Sops version that is targeted.
ARG SOPS_VERSION_ARG=3.11.0
# Set SOPS_TAG=main to use default branch (latest). Make sure SOPS_VERSION_ARG corresponds with Version in https://github.com/getsops/sops/blob/main/version/version.go
# Set SOPS_TAG=v${SOPS_VERSION_ARG} to get tagged version
ARG SOPS_TAG=v${SOPS_VERSION_ARG}

### Build app ###
FROM ${JVM_BUILD_IMAGE} AS build

# Get security updates
RUN apk upgrade --no-cache

COPY . .
RUN ./gradlew build -x test -x smokeTest --no-daemon -Dorg.gradle.jvmargs="-Xmx1024m"

### Build Sops ###
FROM --platform=$BUILDPLATFORM ${SOPS_BUILD_IMAGE} AS sops_build

# Use ARGs from --platform
ARG TARGETOS
ARG TARGETARCH

# Repeat ARG to use it locally
ARG SOPS_TAG
# Convert ARG to ENV to use in cmd
ENV SOPS_BRANCH=${SOPS_TAG}

# Set working directory inside the container
WORKDIR /build

# Install git
RUN apt-get update && apt-get install -y --no-install-recommends git \
    && rm -rf /var/lib/apt/lists/*

# Clone the sops repository for selected branch/tag. Turn off warning for detached head, and skip history
RUN git config --global advice.detachedHead false && \
    git clone --depth 1 --branch ${SOPS_BRANCH} https://github.com/getsops/sops.git

# Change working directory to sops source
WORKDIR /build/sops/cmd/sops

# Build for selected arcitecure using go compiler.
# CGO_ENABLED=0 means you got a statically linked binary, i.e. without any external dependencies
RUN CGO_ENABLED=0 GOOS=${TARGETOS} GOARCH=${TARGETARCH} go install .

### Assemble runtime image ###
FROM ${JRE_IMAGE}

# Get security updates for Alpine packages
RUN apk upgrade --no-cache

# Add non-root user and set permissions.
RUN mkdir /app /app/logs /app/tmp && \
    adduser -D user && chown -R user:user /app /app/logs /app/tmp

# Copy jars, remove *plain.jar, and rename runnable jar
COPY --from=build /build/libs/*.jar /app/
RUN rm /app/*-plain.jar && mv /app/*.jar /app/backend.jar

# Sops version is checked in the actuator/health endpoint in the app
# The health check will fail if sops cannot be run, or has an unexpected version
ARG SOPS_VERSION_ARG
ENV SOPS_VERSION=${SOPS_VERSION_ARG}

# Copy SOPS binary and set it as executable
COPY --from=sops_build /go/bin/sops /usr/bin/sops
RUN chmod +x /usr/bin/sops

# Switch to non-root user.
USER user

# Port 8081 for actuator endpoints
EXPOSE 8080 8081
ENTRYPOINT ["java", "-jar", "/app/backend.jar"]

# Use the health endpoint of the application to provide information through docker about the health state of the application
HEALTHCHECK --start-period=30s --interval=5m \
   CMD wget -O - --quiet --tries=1 http://localhost:8083/actuator/health | grep UP || exit 1
