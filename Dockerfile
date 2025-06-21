ARG BUILD_IMAGE=eclipse-temurin:23.0.2_7-jdk-alpine-3.21
ARG IMAGE=eclipse-temurin:23.0.2_7-jre-alpine-3.21
ARG SOPS_BUILD_IMAGE=golang:1.24.4

# Set SOPS_TAG=main to get latest
ARG SOPS_TAG=v3.10.2

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

# Build the project using make
RUN make install

FROM ${BUILD_IMAGE} AS build

# Get security updates
RUN apk upgrade --no-cache

COPY . .
RUN ./gradlew build -x test -x smokeTest

FROM ${IMAGE}

COPY --from=sops_build /go/bin/sops /usr/local/bin/sops
RUN chmod +x /usr/local/bin/sops

# Add non-root user and change permissions.
RUN mkdir /app /app/logs /app/tmp && \
    adduser -D user && chown -R user:user /app /app/logs /app/tmp

COPY --from=build /build/libs/*.jar /app/backend.jar

# Switch to non-root user.
USER user

EXPOSE 8080 8081
ENTRYPOINT ["java", "-jar", "/app/backend.jar"]

# Use the health endpoint of the application to provide information through docker about the health state of the application
HEALTHCHECK --start-period=30s --interval=5m \
   CMD wget -O - --quiet --tries=1 http://localhost:8083/actuator/health | grep UP || exit 1
