#!/usr/bin/env bash
# +--------------------------------------------------------------+
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
# +--------------------------------------------------------------+

set -euo pipefail

if ! command -v bazel >/dev/null; then
      echo "[noel/tooling:repin-maven-install] Missing \`bazel\` command"
fi

workdir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")"/.. &> /dev/null && pwd)
files=$(find "$workdir" -name "*.java" -type f -print)

exec find "$workdir" -name "*.java" -type f -print | xargs bazel run //tools:google-javaformat -- --replace
