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

import org.gradle.internal.os.OperatingSystem

rootProject.name = "Noel"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("./gradle/build.versions.toml"))
        }
    }
}

includeBuild("build-logic")
include(
    ":bot",
    ":common",
    ":framework",
    ":modules",
    ":modules:config",
    ":modules:discord",
    ":modules:elasticsearch",
    ":modules:kubernetes",
    ":modules:linear",
    ":modules:logging",
    ":modules:metrics",
    ":modules:postgresql"
)

@Suppress("INACCESSIBLE_TYPE")
val validOs = listOf(OperatingSystem.LINUX, OperatingSystem.MAC_OS, OperatingSystem.WINDOWS).map { it.familyName }
val validArch = listOf("amd64", "arm64")

gradle.settingsEvaluated {
    val os = OperatingSystem.current()
    if (!validOs.contains(os.familyName)) {
        throw GradleException(
            """
        |Noel can only be developed and compiled on Windows, macOS, or Linux. Received ${os.familyName} (${os.version}),
        |which is not a valid platform to develop on.
        """.trimMargin("|"),
        )
    }

    val arch = when (System.getProperty("os.arch")) {
        "amd64", "x86_64" -> "amd64"
        "aarch64", "arm64" -> "arm64"
        else -> "unknown"
    }

    if (!validArch.contains(arch)) {
        throw GradleException("Building or developing charted-server on architecture [${System.getProperty("os.arch")}] is not supported")
    }
}
