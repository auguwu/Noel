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

import dev.floofy.noel.gradle.VERSION
import dev.floofy.utils.gradle.by
import java.text.SimpleDateFormat
import java.util.*

plugins {
    id("noel-module")
    application
}

dependencies {
    implementation(projects.modules.logging)
    implementation(projects.modules.discord)
    implementation(projects.framework)
    implementation(projects.modules)

    implementation(libs.jda) {
        exclude(module = "opus-java")
    }
}

application {
    mainClass by "dev.floofy.noel.bot.Bootstrap"
}

distributions {
    main {
        distributionBaseName by "noel"
        contents {
            into("bin") {
                from("$projectDir/bin/noel")
            }
        }
    }
}

tasks {
    processResources {
        filesMatching("build-info.json") {
            val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")
            expand(
                mapOf(
                    "version" to "$VERSION",
                    "commit_sha" to VERSION.gitCommitHash!!.trim(),
                    "build_date" to formatter.format(Date()),
                ),
            )
        }
    }

    startScripts {
        enabled = false
    }
}
