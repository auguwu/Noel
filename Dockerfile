# 🐾✨ Noel: Discord bot made to manage my servers, made in Java.
# Copyright 2021-2023 Noel <cutie@floofy.dev>
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

FROM --platform=$BUILDPLATFORM eclipse-temurin:17.0.7_7-jdk-alpine AS jdk-runtime

RUN apk update && \
    apk add --no-cache binutils && \
    jlink --add-modules ALL-MODULE-PATH \
            --strip-debug \
            --no-man-pages \
            --no-header-files \
            --compress=2 \
            --output /runtime

FROM --platform=$BUILDPLATFORM eclipse-temurin:17.0.7_7-jdk-alpine AS gradle-build

RUN apk update && apk add --no-cache git ca-certificates gcompat libc6-compat curl && apk add --no-cache protobuf-dev --repository=https://dl-cdn.alpinelinux.org/alpine/edge/main
WORKDIR /build

COPY . .
RUN chmod +x ./gradlew && ./gradlew :bot:installDist --no-daemon --stacktrace

FROM --platform=$BUILDPLATFORM alpine:3.18

RUN apk update && apk add --no-cache bash tini libc6-compat gcompat
WORKDIR /app/noel/bot

ENV JAVA_HOME=/opt/openjdk/java
COPY --from=gradle-build /build/bot/build/install/noel/lib  /app/noel/bot/lib
COPY --from=gradle-build /build/bot/build/install/noel/bin  /app/noel/bot/bin
COPY --from=jdk-runtime  /runtime                           /opt/openjdk/java

RUN addgroup -g 1001 noel && \
    adduser -DSH -u 1001 -G noel noel && \
    chown -R noel:noel /app/noel/bot && \
    chmod +x /app/noel/bot/bin/noel

USER noel
CMD ["/app/noel/bot/bin/noel"]
