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

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.diffplug.spotless") version "6.19.0"
    `kotlin-dsl`
}

group = "dev.floofy.noel.gradle"
version = "0.0.0-devel.0"

repositories {
    maven("https://maven.floofy.dev/repo/releases")
    maven("https://maven.noelware.org")
    gradlePluginPortal()
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("com.diffplug.spotless:spotless-plugin-gradle:6.19.0")
    implementation("dev.floofy.commons:gradle:2.5.1")
    implementation(gradleApi())
}

kotlin {
    jvmToolchain(17)
}

spotless {
    kotlin {
        endWithNewline()
        encoding("UTF-8")
        target("**/*.kt")

        ktlint().apply {
            setEditorConfigPath(file("${rootProject.projectDir}/../.editorconfig"))
            setUseExperimental(true)
        }

        // https://github.com/diffplug/spotless/issues/1599
        licenseHeaderFile(file("${rootProject.projectDir}/../assets/HEADING"))
    }

    kotlinGradle {
        endWithNewline()
        encoding("UTF-8")
        target("**/*.kt")

        ktlint().apply {
            setEditorConfigPath(file("${rootProject.projectDir}/../.editorconfig"))
            setUseExperimental(true)
        }

        // https://github.com/diffplug/spotless/issues/1599
        licenseHeaderFile(file("${rootProject.projectDir}/../assets/HEADING"), "(package |@file|import |pluginManagement|plugins|rootProject.name)")
    }
}

tasks {
    withType<KotlinCompile>().configureEach {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
        compilerOptions.javaParameters.set(true)
    }
}

configurations.configureEach {
    if (isCanBeResolved) {
        attributes {
            attribute(
                GradlePluginApiVersion.GRADLE_PLUGIN_API_VERSION_ATTRIBUTE,
                project.objects.named(GradleVersion.current().version)
            )
        }
    }
}
