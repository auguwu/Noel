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

import com.diffplug.gradle.spotless.SpotlessExtensionPredeclare
import dev.floofy.noel.gradle.*
import dev.floofy.utils.gradle.*

buildscript {
    repositories {
        maven("https://maven.floofy.dev/repo/releases")
        maven("https://maven.noelware.org")
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
    }

    dependencies {
        classpath("dev.floofy.noel.gradle:build-logic:0.0.0-devel.0")
    }
}

plugins {
    id("com.diffplug.spotless") version "6.19.0"
    application
}

group = "org.noelware.charted"
version = "$VERSION"
description = "\uD83D\uDC3E✨ Discord bot made to manage my servers, made in Java"

repositories {
    mavenCentral()
    mavenLocal()
}

spotless {
    // Use the line ending from .gitattributes
    // WINDOWS: You will need to run `git config --global core.autocrlf input` to not have conversions
    //          to take into account.
    lineEndings = com.diffplug.spotless.LineEnding.GIT_ATTRIBUTES

    predeclareDeps()
    encoding("UTF-8")
    format("prettier") {
        target(
            "**/*.json",
            "**/*.yaml",
            "**/*.yml",
            "**/*.md",
        )

        prettier(mapOf("prettier" to "2.8.4")).apply {
            configFile(file("$projectDir/.prettierrc.json"))
        }
    }

    kotlinGradle {
        targetExclude(
            "build-logic/build/kotlin-dsl/plugins-blocks/extracted/noel-module.gradle.kts",
        )

        endWithNewline()
        encoding("UTF-8")
        target("**/*.gradle.kts")
        ktlint().apply {
            setUseExperimental(true)
            setEditorConfigPath(file("${rootProject.projectDir}/.editorconfig"))
        }

        licenseHeaderFile(file("${rootProject.projectDir}/assets/HEADING"), "(package |@file|plugins|pluginManagement|import|rootProject)")
    }
}

the<SpotlessExtensionPredeclare>().apply {
    kotlinGradle {
        ktlint()
    }

    kotlin {
        ktlint()
    }

    java {
        googleJavaFormat()
        palantirJavaFormat()
    }
}

java {
    toolchain {
        languageVersion by JavaLanguageVersion.of(JAVA_VERSION.majorVersion)
    }
}

tasks {
    wrapper {
        distributionType = Wrapper.DistributionType.ALL
    }

    create<Copy>("precommitHook") {
        from(file("${project.rootDir}/scripts/pre-commit"))
        into(file("${project.rootDir}/.git/hooks"))
    }

    named("spotlessCheck") {
        dependsOn(gradle.includedBuilds.map { it.task(":spotlessCheck") })
    }

    named("spotlessApply") {
        dependsOn(gradle.includedBuilds.map { it.task(":spotlessApply") })
    }
}
