# 🐾✨ Noel: Discord bot made to manage my servers made in Java
# Copyright 2021-2026 Noel Towa <cutie@floofy.dev>, et al.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

FROM debian:trixie-slim@sha256:f6b08b09a05725f9431502d3b5714f5c4576e82c1a84fa1debe0c24a7e0c3ec4 AS glibc

# TODO(@auguwu/Noel): Switch to `cr.noelware.cloud/bazel/bazel:${BAZEL_VERSION}-alpine` once available
FROM frolvlad/alpine-glibc:alpine-3.22_glibc-2.42@sha256:6f39887bfd345165186486edbda2702bbf111e77941dab1bdebad8c34ca8d35d AS bazel

RUN apk upgrade && apk add --no-cache build-base python3 bash unzip zip curl linux-headers

COPY --from=glibc "/usr/lib/x86_64-linux-gnu/libstdc++.so.6*" /usr/glibc-compat/lib/
COPY --from=glibc "/usr/lib/x86_64-linux-gnu/libgcc_s.so.1"   /usr/glibc-compat/lib/
RUN /usr/glibc-compat/sbin/ldconfig

ARG BAZEL_VERSION="9.3.1"
ENV TEMURIN_VERSION="25.0.5+8"
ENV JAVA_HOME="/opt/eclipse/temurin"

RUN mkdir -p $JAVA_HOME \
    && curl -Lo /tmp/temurin.tgz "https://github.com/adoptium/temurin25-binaries/releases/download/jdk-${TEMURIN_VERSION}/OpenJDK25U-jdk_x64_alpine-linux_hotspot_25.0.5_8.tar.gz" \
    && tar -xzf /tmp/temurin.tgz -C $JAVA_HOME --strip-components=1 \
    && rm /tmp/temurin.tgz

ENV PATH="$JAVA_HOME/bin:$PATH"
RUN curl -fSL https://github.com/bazelbuild/bazel/releases/download/${BAZEL_VERSION}/bazel-${BAZEL_VERSION}-dist.zip -o /tmp/bazeldist.zip \
    && unzip -q /tmp/bazeldist.zip -d /build \
    && rm /tmp/bazeldist.zip

WORKDIR /build
ENV EXTRA_BAZEL_ARGS="--tool_java_runtime_version=local_jdk --java_runtime_version=local_jdk \
    --java_language_version=25 --tool_java_language_version=25 \
    --copt=-D_LARGEFILE64_SOURCE --host_copt=-D_LARGEFILE64_SOURCE \
    --nobuild_python_zip --verbose_failures --announce_rc --curses=no"

RUN env JAVA_VERSION=25 bash ./compile.sh
RUN mv /build/output/bazel /usr/bin/bazel

FROM bazel AS build

COPY . /build
WORKDIR /build

ENV EXTRA_BAZEL_ARGS="--tool_java_runtime_version=local_jdk --java_runtime_version=local_jdk"
RUN bazel --bazelrc=.dockerbuild.bazelrc build //bot:bootstrap_deploy.jar --config=opt

FROM eclipse-temurin:25.0.4.1_1-jdk-alpine-3.24@sha256:1e20ead9eec7234db293f1913438090d533ed47c0b9e12dc8c7627a802a44ed6 AS jdk-runtime

COPY --from=build /build/bazel-bin/bot/bootstrap_deploy.jar /tmp/bot.jar
RUN apk update && \
    apk add --no-cache binutils && \
    jlink \
          --add-modules "$(jdeps --ignore-missing-deps --print-module-deps --multi-release 25 /tmp/bot.jar),jdk.unsupported,jdk.attach,jdk.crypto.cryptoki" \
          --strip-debug --no-man-pages --no-header-files \
          --compress=zip-6 \
          --output /runtime

FROM alpine:3.24@sha256:be2656061b536c1883c4034790e21d0a09c5aaaf51968b89368a3c0371ea2c9a

RUN apk update && apk add --no-cache bash tini libc6-compat gcompat file
WORKDIR /app/noel

ENV JAVA_HOME=/opt/eclipse/temurin-25
COPY --from=jdk-runtime /runtime                                  /opt/eclipse/temurin-25
COPY --from=build       /build/bazel-bin/bot/bootstrap_deploy.jar /app/noel/lib/Noel.jar
COPY                    distribution/launcher                     /app/noel/bin/noel

RUN addgroup -g 1001 noel && \
    adduser -DSH -u 1001 -G noel noel && \
    chown -R noel:noel /app/noel && \
    chmod +x /app/noel/bin/noel

USER noel
CMD ["/app/noel/bin/noel"]
