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

# TODO(@auguwu/Noel): switch to cr.noel.pink/bazelbuild/bazel:8.5.1-alpine once available
FROM frolvlad/alpine-glibc:alpine-3.22_glibc-2.42 AS bazelbuild

ARG BAZEL_VERSION=8.5.1

RUN apk upgrade && apk add --no-cache build-base python3 bash unzip zip curl linux-headers

ENV TEMURIN_VERSION=21.0.10+7
ENV JAVA_HOME=/opt/eclipse/temurin

RUN mkdir -p $JAVA_HOME \
    && curl -L -o /tmp/temurin.tar.gz \
        https://github.com/adoptium/temurin21-binaries/releases/download/jdk-${TEMURIN_VERSION}/OpenJDK21U-jdk_x64_alpine-linux_hotspot_21.0.10_7.tar.gz \
    && tar -xzf /tmp/temurin.tar.gz -C $JAVA_HOME --strip-components=1 \
    && rm /tmp/temurin.tar.gz

ENV PATH="$JAVA_HOME/bin:$PATH"

RUN curl -fSL https://github.com/bazelbuild/bazel/releases/download/${BAZEL_VERSION}/bazel-${BAZEL_VERSION}-dist.zip -o /tmp/bazeldist.zip \
    && unzip -q /tmp/bazeldist.zip -d /build \
    && rm /tmp/bazeldist.zip

WORKDIR /build

ENV EXTRA_BAZEL_ARGS="--tool_java_runtime_version=local_jdk --nobuild_python_zip --verbose_failures --announce_rc --curses=no"
RUN env JAVA_VERSION=21 bash ./compile.sh && exit 1

FROM eclipse-temurin:21.0.10_7-jdk-alpine-3.23 AS build

FROM eclipse-temurin:21.0.10_7-jdk-alpine-3.23