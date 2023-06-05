/*
 * 🐾✨ Noel: Discord bot made to manage my servers, made in Java.
 * Copyright 2021-2023 Noel <cutie@floofy.dev>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import dev.floofy.noel.gradle.*
import dev.floofy.utils.gradle.*
import java.text.SimpleDateFormat
import java.util.*

plugins {
    id("com.diffplug.spotless")

    `java-library`
    java
    idea
}

group = "dev.floofy.noel"
version = "$VERSION"
description = "\uD83D\uDC3E✨ Discord bot made to manage my servers, made in Java."

repositories {
    mavenCentral()
    mavenLocal()
    noel()
}

dependencies {
    // Java Annotations
    implementation(libs.library("jetbrains-annotations"))

    if (path != ":common") {
        implementation(project(":common"))
    }

    // noel commons
    api(libs.library("noel-commons-extensions-kotlin"))
    api(libs.library("noel-commons-java-utils"))

    // SLF4J
    api(libs.library("slf4j-api"))

    // Apache Utilities
    api(libs.library("apache-commons-lang3"))

    // Sentry
    api(libs.library("sentry"))

    // Jackson
    api(libs.library("jackson-databind"))
}

spotless {
    java {
        licenseHeaderFile(file("${rootProject.projectDir}/assets/HEADING"))
        trimTrailingWhitespace()
        removeUnusedImports()
        palantirJavaFormat()
        endWithNewline()
        encoding("UTF-8")
    }
}

java {
    toolchain {
        languageVersion by JavaLanguageVersion.of(JAVA_VERSION.majorVersion)
    }
}

// This will transform the project path:
//
//    - :bot -> noel-bot-{VERSION}.jar
//    - :modules:postgresql -> noel-modules-postgresql-{VERSION}.jar
val projectName: String = path
    .substring(1)
    .replace(':', '-')

val RFC3339Formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
tasks {
    withType<Jar> {
        archiveFileName by "noel-$projectName-$VERSION.jar"
        manifest {
            attributes(
                mapOf(
                    "Implementation-Build-Date" to RFC3339Formatter.format(Date()),
                    "Implementation-Version" to "$VERSION",
                    "Implementation-Vendor" to "Noel Towa <cutie@floofy.dev>",
                    "Implementation-Title" to "noel-$projectName",
                    "Created-By" to GradleVersion.current(),
                ),
            )
        }
    }

    withType<JavaCompile>().configureEach {
        options.isIncremental = true
        options.encoding = "UTF-8"
        options.isFork = true
    }
}
