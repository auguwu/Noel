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

load("@contrib_rules_jvm//java:defs.bzl", "java_junit5_test")
load("@rules_jvm_external//:defs.bzl", "artifact")

JUNIT5_RUNTIME_DEPS = [
    artifact("org.junit.platform:junit-platform-launcher"),
    artifact("org.junit.platform:junit-platform-reporting"),
    artifact("org.junit.jupiter:junit-jupiter-engine"),
    artifact("org.slf4j:slf4j-simple"),
]

def noel_java_test(
        name,
        size = "small",
        deps = [],
        runtime_deps = [],
        visibility = ["//visibility:private"],
        **kwargs):
    return java_junit5_test(
        name = name,
        size = size,
        deps = [
            artifact("org.junit.jupiter:junit-jupiter-api"),
            artifact("org.assertj:assertj-core")
        ] + deps,
        runtime_deps = JUNIT5_RUNTIME_DEPS + runtime_deps,
        visibility = visibility,
        **kwargs
    )
