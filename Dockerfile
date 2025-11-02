# ---- Base images (Eclipse Temurin on UBI9 Minimal = fresher CVE profile) ----
ARG JVM_BUILD_IMAGE=eclipse-temurin:24.0.2_12-jdk-ubi9-minimal
ARG JRE_IMAGE=eclipse-temurin:24.0.2_12-jre-ubi9-minimal
ARG SOPS_BUILD_IMAGE=golang:1.24-bookworm

# Sops version that is targeted.
ARG SOPS_VERSION_ARG=3.11.0
ARG SOPS_TAG=v${SOPS_VERSION_ARG}

### Build app ###
FROM ${JVM_BUILD_IMAGE} AS build

USER root
WORKDIR /build

# xargs for your build
RUN microdnf -y update && microdnf -y install findutils && microdnf clean all

# Create a real home and make everything writable by the build user
RUN useradd -m -d /home/appuser -r -u 10001 appuser && \
    # make sure any cached /build content (incl. .kotlin) is owned & writable
    chown -R 10001:0 /build /home/appuser && chmod -R g+rwX /build /home/appuser

# Let Gradle & Kotlin use writable locations
ENV HOME=/home/appuser \
    GRADLE_USER_HOME=/home/appuser/.gradle \
    KOTLIN_COMPILER_DATADIR=/home/appuser/.kotlin

# Optional: cooperative umask so group-writable survives
RUN printf 'umask 0002\n' >> /etc/profile

USER 10001

# Ensure sources arrive owned by appuser to avoid root-owned files
COPY --chown=10001:0 . .

# Create the dirs Kotlin/Gradle will use (in case the project cleans them)
RUN mkdir -p "$GRADLE_USER_HOME" "$KOTLIN_COMPILER_DATADIR"

ENV GRADLE_OPTS="-Dorg.gradle.jvmargs=-Xmx1024m"
RUN ./gradlew --version >/dev/null && \
    ./gradlew build -x test -x smokeTest --no-daemon

# =============================== Build SOPS ==================================
FROM --platform=$BUILDPLATFORM ${SOPS_BUILD_IMAGE} AS sops_build

# Use ARGs from --platform
ARG TARGETOS
ARG TARGETARCH
ARG SOPS_TAG

# Minimal deps; keep layer clean
USER root
RUN apt-get update && apt-get install -y --no-install-recommends git ca-certificates \
  && rm -rf /var/lib/apt/lists/*

# Less noisy git, shallow clone
WORKDIR /src
RUN git config --global advice.detachedHead false && \
    git clone --depth 1 --branch "${SOPS_TAG}" https://github.com/getsops/sops.git

# Build static SOPS for the target platform
WORKDIR /src/sops/cmd/sops
RUN CGO_ENABLED=0 GOOS=${TARGETOS} GOARCH=${TARGETARCH} \
    go build -trimpath -ldflags="-s -w" -o /out/sops .
# ===================== Tiny wget just for HEALTHCHECK ========================
FROM busybox:1.37.0-glibc AS bb

# =============================== Runtime image ===============================
# =============================== Runtime image ===============================
FROM ${JRE_IMAGE}

ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport"

# Create runtime user & dirs
USER root
RUN useradd -r -u 10001 appuser && \
    mkdir -p /app /app/logs /app/tmp && chown -R 10001:0 /app

# App jar: do as appuser
USER 10001
COPY --from=build /build/build/libs/*.jar /app/
RUN sh -c 'set -e; for f in /app/*-plain.jar; do [ -e "$f" ] && rm -f "$f"; done; \
           jar=$(echo /app/*.jar | awk "{print \$1}"); mv "$jar" /app/backend.jar'

# ---- back to root to place binaries into /usr/bin ----
USER root

# SOPS: set perms during copy (no chmod later)
COPY --from=sops_build --chown=root:root --chmod=0755 /out/sops /usr/bin/sops

# BusyBox wget for healthcheck: set perms during copy, then symlink
COPY --from=bb --chown=root:root --chmod=0755 /bin/busybox /usr/bin/busybox
RUN ln -sf /usr/bin/busybox /usr/bin/wget

# Drop privileges for runtime
USER 10001

EXPOSE 8080 8081
ENTRYPOINT ["java", "-jar", "/app/backend.jar"]

HEALTHCHECK --start-period=30s --interval=5m --timeout=10s \
  CMD /usr/bin/wget -qO- http://localhost:8081/actuator/health | grep -q '"status":"UP"' || exit 1
